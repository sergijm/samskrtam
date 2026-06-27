package sm.selflearn.samskrtam.gateway.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.gateway.config.OAuth2Properties;
import sm.selflearn.samskrtam.gateway.oauth2.KeycloakTokenResponse; // Re-using this DTO

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OAuth2Properties oauth2Props;

    @Qualifier("keycloakWebClient")
    private final WebClient keycloakWebClient;

    @PostMapping("/login")
    public Mono<KeycloakTokenResponse> login(@RequestBody LoginRequest loginRequest) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", oauth2Props.getClientId());
        formData.add("client_secret", oauth2Props.getClientSecret());
        formData.add("username", loginRequest.username());
        formData.add("password", loginRequest.password());
        formData.add("scope", "openid email profile offline_access");
        log.debug("Attempting direct login with Keycloak for user: {}", loginRequest.username());

        return keycloakWebClient.post()
                .uri(oauth2Props.tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error(
                                        "Keycloak direct login failed with 4xx status: status={}, body={}",
                                        response.statusCode(), body))
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED, "Authentication failed: " + body))))
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error(
                                        "Keycloak direct login failed with error status: status={}, body={}",
                                        response.statusCode(), body))
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        response.statusCode(), "Authentication failed: " + body))))
                .bodyToMono(KeycloakTokenResponse.class)
                .doOnSuccess(r -> log.debug("Keycloak direct login successful for user: {}", loginRequest.username()))
                .doOnError(e -> log.error("Error during Keycloak direct login: {}", e.getMessage(), e));
    }

    @PostMapping("/refresh")
    public Mono<KeycloakTokenResponse> refresh(@RequestBody RefreshRequest refreshRequest) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", oauth2Props.getClientId());
        formData.add("client_secret", oauth2Props.getClientSecret());
        formData.add("refresh_token", refreshRequest.refreshToken());

        log.debug("Attempting token refresh");

        return keycloakWebClient.post()
                .uri(oauth2Props.tokenEndpoint())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .onStatus(status -> status.is4xxClientError(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error(
                                        "Keycloak token refresh failed with 4xx: status={}, body={}",
                                        response.statusCode(), body))
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        HttpStatus.UNAUTHORIZED, "Token refresh failed: " + body))))
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .doOnNext(body -> log.error(
                                        "Keycloak token refresh failed with error: status={}, body={}",
                                        response.statusCode(), body))
                                .flatMap(body -> Mono.error(new ResponseStatusException(
                                        response.statusCode(), "Token refresh failed: " + body))))
                .bodyToMono(KeycloakTokenResponse.class)
                .doOnSuccess(r -> log.debug("Token refresh successful"))
                .doOnError(e -> log.error("Error during token refresh: {}", e.getMessage(), e));
    }

    private record LoginRequest(
            @JsonProperty("username") String username,
            @JsonProperty("password") String password
    ) {}

    private record RefreshRequest(
            @JsonProperty("refresh_token") String refreshToken
    ) {}
}

