package sm.selflearn.samskrtam.gateway.oauth2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import sm.selflearn.samskrtam.user.dto.OAuthSyncRequest;

import java.net.URI;
import java.util.Set;

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

    @GetMapping("/{provider}")
    public Mono<ResponseEntity<Void>> initiateOAuth2(
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

                    return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(authUrl))
                            .build());
                });
    }

    @GetMapping("/callback")
    public Mono<ResponseEntity<Void>> handleCallback(
            @RequestParam String code,
            @RequestParam String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            ServerWebExchange exchange) {

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
                                .thenReturn(keycloakToken)) // Return the whole token response
                        .doOnNext(finalAppToken -> log.debug("Final appToken for redirect: {}", finalAppToken))
                        .flatMap(appToken -> redirectToFrontendWithTokens(exchange, appToken))) // Pass the whole token response
                .doOnSuccess(v -> log.debug("OAuth2 callback processing completed successfully."))
                .doOnError(e -> log.error("Error during OAuth2 callback processing: {}", e.getMessage(), e));
    }

    private String buildAuthorizationUrl(String provider, String state) {
        return oauth2Props.oidcAuthorizationEndpoint()
               + "?client_id=" + oauth2Props.getClientId()
               + "&redirect_uri=" + encodeUrl(oauth2Props.getRedirectUri())
               + "&response_type=code"
               + "&scope=openid+email+profile+offline_access"
               + "&state=" + state
               + "&kc_idp_hint=" + provider;
    }

    private Mono<KeycloakTokenResponse> exchangeCodeForTokens(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",    "authorization_code");
        form.add("code",          code);
        form.add("redirect_uri",  oauth2Props.getRedirectUri());
        form.add("client_id",     oauth2Props.getClientId());
        form.add("client_secret", oauth2Props.getClientSecret());

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
                .onStatus(status -> status.isError(), response ->
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

    private Mono<Void> syncProfileWithUserService(String keycloakAccessToken, String provider) {
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
                .bodyToMono(Void.class)
                .doOnSuccess(t -> log.debug("user-service oauth2 sync successful"));
    }

    private Mono<ResponseEntity<Void>> redirectToFrontendWithTokens(ServerWebExchange exchange, KeycloakTokenResponse tokenResponse) {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            log.error("Frontend URL is not configured. Cannot redirect after OAuth2 callback.");
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Frontend URL not configured"));
        }

        String location = frontendUrl + "/auth/callback#access_token=" + tokenResponse.accessToken() + "&refresh_token=" + tokenResponse.refreshToken();
        log.debug("Redirecting to frontend with tokens. Location: {}", location);
        try {
            return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(location))
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Failed to create URI for frontend redirect. Location: {}. Error: {}",
                    location, e.getMessage());
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Invalid frontend redirect URL"));
        }
    }

    private Mono<ResponseEntity<Void>> redirectToFrontendWithError(ServerWebExchange exchange, String error) {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            log.error("Frontend URL is not configured. Cannot redirect after OAuth2 error.");
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Frontend URL not configured for error redirect"));
        }
        String location = frontendUrl + "/auth/callback?error=" + encodeUrl(error);
        log.debug("Redirecting to frontend with error. Location: {}", location);
        try {
            return Mono.just(ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(location))
                    .build());
        } catch (IllegalArgumentException e) {
            log.error("Failed to create URI for frontend error redirect. Location: {}. Error: {}",
                    location, e.getMessage());
            return Mono.error(new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Invalid frontend redirect URL"));
        }
    }

    private String encodeUrl(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
