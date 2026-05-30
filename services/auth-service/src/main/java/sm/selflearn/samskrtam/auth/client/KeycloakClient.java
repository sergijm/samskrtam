package sm.selflearn.samskrtam.auth.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class KeycloakClient {

    private final RestClient restClient;
    private final String tokenUrl;

    public KeycloakClient(
            @Value("${keycloak.url}") String keycloakUrl,
            @Value("${keycloak.realm}") String realm
    ) {
        this.tokenUrl = keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token";
        this.restClient = RestClient.builder().build();
    }

    public Map<String, Object> exchangeCode(String clientId, String clientSecret, String code, String redirectUri) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("code", code);
        formData.add("redirect_uri", redirectUri);
        return postToTokenEndpoint(formData);
    }

    public Map<String, Object> login(String clientId, String clientSecret, String email, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", email);
        formData.add("password", password);
        formData.add("scope", "openid email profile");
        return postToTokenEndpoint(formData);
    }

    public Map<String, Object> refresh(String clientId, String clientSecret, String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);
        return postToTokenEndpoint(formData);
    }

    private Map<String, Object> postToTokenEndpoint(MultiValueMap<String, String> formData) {
        return restClient.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(Map.class);
    }
}
