package sm.selflearn.samskrtam.user.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminTokenProvider {

    private final RestClient restClient;

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id-admin}")
    private String adminClientId;

    @Value("${keycloak.client-secret-admin}")
    private String adminClientSecret;

    private String adminToken;
    private Instant tokenExpiry;

    public String getToken() {
        if (adminToken == null || tokenExpiry == null || tokenExpiry.minusSeconds(30).isBefore(Instant.now())) {
            log.debug("Admin token expired or not present, requesting new one.");
            refreshAdminToken();
        }
        return adminToken;
    }

    private void refreshAdminToken() {
        try {
            // Change Map<String, String> to Map<String, Object> to correctly handle Integer for expires_in
            Map<String, Object> response = restClient.post()
                    .uri(keycloakUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body("grant_type=client_credentials&client_id=" + adminClientId + "&client_secret=" + adminClientSecret)
                    .retrieve()
                    .body(Map.class);

            adminToken = (String) response.get("access_token");
            Integer expiresIn = (Integer) response.get("expires_in"); // Cast to Integer
            tokenExpiry = Instant.now().plusSeconds(expiresIn);
            log.info("Successfully obtained new Keycloak admin token, expires in {} seconds.", expiresIn);
        } catch (Exception e) {
            log.error("Failed to obtain Keycloak admin token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to obtain Keycloak admin token", e);
        }
    }
}
