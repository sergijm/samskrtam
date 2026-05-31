package sm.selflearn.samskrtam.gateway.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity; // Added import
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.gateway.config.OAuth2Properties;

import java.net.URI;
import java.util.Set;

/**
 * Обрабатывает OAuth2 Authorization Code flow.
 *
 * <p>Фронтенд никогда не знает client_secret — он хранится только в env Gateway.
 *
 * <h3>Эндпоинты:</h3>
 * <ul>
 *   <li>{@code GET /api/v1/auth/oauth2/{provider}} — инициирует редирект на Keycloak
 *   <li>{@code GET /api/v1/auth/oauth2/callback} — принимает code от Keycloak,
 *       обменивает на токены, синхронизирует профиль с user-service,
 *       редиректит фронтенд с токеном в URL fragment
 * </ul>
 *
 * <p>Токен передаётся в URL fragment (#) — браузер не отправляет fragment серверу,
 * поэтому токен не попадает в серверные логи.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth/oauth2")
@RequiredArgsConstructor
public class OAuthController {

    private static final Set<String> SUPPORTED_PROVIDERS = Set.of("google", "mailru");

    private final OAuth2Properties   oauth2Props;
    private final OAuthStateService  stateService;

    @Qualifier("keycloakWebClient")
    private final WebClient keycloakWebClient;

    @Qualifier("userServiceWebClient")
    private final WebClient userServiceWebClient;

    @Value("${frontend.url}")
    private String frontendUrl;

    // ── Step 1: Инициация OAuth2 flow ────────────────────────────────────────

    /**
     * Фронтенд вызывает этот эндпоинт для начала OAuth2 flow.
     *
     * <p>Gateway:
     * <ol>
     *   <li>Валидирует provider
     *   <li>Генерирует cryptographically secure state, сохраняет в Redis (TTL 10 мин)
     *   <li>Строит Authorization URL к Keycloak
     *   <li>Возвращает 302 Redirect на Keycloak
     * </ol>
     */
    @GetMapping("/{provider}")
    public Mono<ResponseEntity<Void>> initiateOAuth2( // Changed return type
            @PathVariable String provider,
            ServerWebExchange exchange) {

        if (!SUPPORTED_PROVIDERS.contains(provider)) {
            return Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported OAuth2 provider: " + provider));
        }

        return stateService.generate(provider)
                .flatMap(state -> {
                    String authUrl = buildAuthorizationUrl(provider, state);
                    log.debug("OAuth2 initiate: provider={}, redirectTo={}", provider, authUrl);

                    // Using ResponseEntity for explicit redirect
                    return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(authUrl))
                            .build());
                });
    }

    // ── Step 5: Callback от Keycloak ─────────────────────────────────────────

    /**
     * Keycloak редиректит сюда с {@code code} и {@code state} после аутентификации.
     *
     * <p>Gateway:
     * <ol>
     *   <li>Проверяет state из Redis (защита от CSRF) — одноразовый
     *   <li>Обменивает code на токены у Keycloak (с client_secret)
     *   <li>Передаёт Keycloak access_token в user-service для синхронизации профиля
     *   <li>Получает собственный JWT от user-service
     *   <li>Редиректит фронтенд: {@code ${FRONTEND_URL}/auth/callback#token=...}
     * </ol>
     *
     * <p>Токен передаётся в URL fragment (#) — браузер не отправляет fragment серверу,
     * поэтому токен не попадает в серверные логи.
     */
    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> handleCallback( // Changed return type
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            ServerWebExchange exchange) {

        // Keycloak вернул ошибку (пользователь отказал в доступу и т.п.)
        if (error != null) {
            log.warn("OAuth2 callback error: error={}, description={}", error, errorDescription);
            return redirectToFrontendWithError(exchange, error);
        }

        return stateService.validateAndConsume(state)
                .switchIfEmpty(Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Invalid or expired OAuth2 state")))
                .flatMap(provider -> exchangeCodeForTokens(code)
                        .flatMap(keycloakToken -> syncProfileWithUserService(
                                keycloakToken.accessToken(), provider)
                                .then(Mono.just(keycloakToken.accessToken()))) // Explicitly pass accessToken
                        .doOnNext(finalAppToken -> log.debug("Final appToken for redirect: {}", finalAppToken))
                        .flatMap(appToken -> redirectToFrontendWithToken(exchange, appToken)))
                .doOnSuccess(v -> log.debug("OAuth2 callback processing completed successfully."))
                .doOnError(e -> log.error("Error during OAuth2 callback processing: {}", e.getMessage(), e));
    }

    // ── Вспомогательные методы ───────────────────────────────────────────────

    /**
     * Строит URL Authorization Endpoint с параметрами.
     * Keycloak Identity Provider alias совпадает с provider slug (google, mailru).
     */
    private String buildAuthorizationUrl(String provider, String state) {
        return oauth2Props.oidcAuthorizationEndpoint()
               + "?client_id=" + oauth2Props.getClientId()
               + "&redirect_uri=" + encodeUrl(oauth2Props.getRedirectUri())
               + "&response_type=code"
               + "&scope=openid+email+profile"
               + "&state=" + state
               + "&kc_idp_hint=" + provider; // Добавляем подсказку провайдера
    }

    /**
     * Шаг 2: Обмен code на токены у Keycloak.
     * client_secret передаётся только здесь — фронтенд его никогда не видит.
     */
    private Mono<KeycloakTokenResponse> exchangeCodeForTokens(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",    "authorization_code");
        form.add("code",          code);
        form.add("redirect_uri",  oauth2Props.getRedirectUri());
        form.add("client_id",     oauth2Props.getClientId());
        form.add("client_secret", oauth2Props.getClientSecret()); // ← только здесь

        log.debug("Attempting to exchange code for tokens with Keycloak. Token Endpoint: {}, Client ID: {}, Redirect URI: {}",
                oauth2Props.tokenEndpoint(), oauth2Props.getClientId(), oauth2Props.getRedirectUri());

        return keycloakWebClient.post()
                .uri(oauth2Props.tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error(
                                        "Keycloak token exchange failed with 4xx status: status={}, body={}",
                                        response.statusCode(), body))
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED, "Token exchange failed: " + body))))
                .onStatus(status -> status.isError(), response -> // Catch other error statuses (5xx)
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error(
                                        "Keycloak token exchange failed with error status: status={}, body={}",
                                        response.statusCode(), body))
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        response.statusCode(), "Token exchange failed: " + body))))
                .bodyToMono(KeycloakTokenResponse.class)
                .doOnSuccess(r -> log.debug("Keycloak token exchange successful"))
                .doOnError(e -> log.error("Error during Keycloak token exchange: {}", e.getMessage(), e));
    }

    /**
     * Шаг 3: Синхронизация профиля с user-service.
     * user-service создаёт/обновляет пользователя и возвращает собственный JWT.
     */
    private Mono<Void> syncProfileWithUserService(String keycloakAccessToken, String provider) { // Changed return type to Mono<Void>
        return userServiceWebClient.post()
                .uri("/api/v1/users/oauth2/sync")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OAuthSyncRequest(keycloakAccessToken, provider))
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error(
                                        "user-service oauth2 sync failed: status={}, body={}",
                                        response.statusCode(), body))
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "Profile sync failed"))))
                .bodyToMono(Void.class) // Expecting no body or Void
                .doOnSuccess(t -> log.debug("user-service oauth2 sync successful"));
    }

    /** Шаг 4: Редирект на фронтенд с токеном в URL fragment. */
    private Mono<ResponseEntity<Void>> redirectToFrontendWithToken(ServerWebExchange exchange, String token) { // Changed return type
        if (frontendUrl == null || frontendUrl.isBlank()) {
            log.error("Frontend URL is not configured. Cannot redirect after OAuth2 callback.");
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Frontend URL not configured"));
        }

        String location = frontendUrl + "/auth/callback#token=" + token;
        log.debug("Redirecting to frontend with token. Frontend URL: {}, Location: {}", frontendUrl, location);
        try {
            return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(location))
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Failed to create URI for frontend redirect. Frontend URL: {}, Location: {}. Error: {}",
                    frontendUrl, location, e.getMessage());
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Invalid frontend redirect URL"));
        }
    }

    /** Редирект на фронтенд при ошибке OAuth2. */
    private Mono<ResponseEntity<Void>> redirectToFrontendWithError(ServerWebExchange exchange, String error) { // Changed return type
        if (frontendUrl == null || frontendUrl.isBlank()) {
            log.error("Frontend URL is not configured. Cannot redirect after OAuth2 error.");
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Frontend URL not configured for error redirect"));
        }
        String location = frontendUrl + "/auth/callback?error=" + encodeUrl(error);
        log.debug("Redirecting to frontend with error. Frontend URL: {}, Location: {}", frontendUrl, location);
        try {
            return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(location))
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Failed to create URI for frontend error redirect. Frontend URL: {}, Location: {}. Error: {}",
                    frontendUrl, location, e.getMessage());
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Invalid frontend error redirect URL"));
        }
    }

    private String encodeUrl(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    // ── Inner DTOs ────────────────────────────────────────────────────────────

    record OAuthSyncRequest(String keycloakToken, String provider) {}
    // Removed OAuthSyncResponse
}
