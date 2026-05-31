package sm.selflearn.samskrtam.gateway.oauth2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO ответа Keycloak /token endpoint.
 * Поля которых нет в ответе — игнорируются.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KeycloakTokenResponse(

        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("token_type")
        String tokenType
) {}
