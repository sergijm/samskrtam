package sm.selflearn.samskrtam.user.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import sm.selflearn.samskrtam.user.model.OutboxEvent;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final AdminTokenProvider tokenProvider;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String realm;

    public void apply(OutboxEvent event) {
        log.trace("apply: eventType={}, aggregateId={}", event.getEventType(), event.getAggregateId());

        String userId = event.getAggregateId().toString();
        Map<String, Object> payload = parsePayload(event.getPayload());

        switch (event.getEventType()) {
            // USER_REGISTERED is handled directly by OutboxProcessor
            case PROFILE_UPDATED -> updateKeycloakUser(userId, payload);
            case USER_BLOCKED    -> setEnabled(userId, false);
            case USER_UNBLOCKED  -> setEnabled(userId, true);
            default -> throw new OutboxProcessingException("Unknown OutboxEventType: " + event.getEventType());
        }
    }

    /**
     * Creates a user in Keycloak and returns the Keycloak user ID (sub).
     * The payload should contain username, email, firstName, lastName, password.
     *
     * @param payload Map containing user details.
     * @return The UUID of the user created in Keycloak.
     */
    public UUID createKeycloakUser(Map<String, Object> payload) {
        Map<String, Object> keycloakUser = new HashMap<>();
        keycloakUser.put("username", payload.get("username"));
        keycloakUser.put("email", payload.get("email"));
        keycloakUser.put("firstName", payload.get("firstName"));
        keycloakUser.put("lastName", payload.get("lastName"));
        keycloakUser.put("enabled", true);
        keycloakUser.put("emailVerified", false);
        keycloakUser.put("credentials", List.of(Map.of(
                "type", "password",
                "value", payload.get("password"),
                "temporary", false
        )));


        ResponseEntity<Void> response = restClient.post()
                .uri(adminUrl() + "/users")
                .header("Authorization", "Bearer " + tokenProvider.getToken())
                .body(keycloakUser)
                .retrieve()
                .toBodilessEntity();

        URI location = response.getHeaders().getLocation();
        if (location == null) {
            throw new OutboxProcessingException("Keycloak did not return a Location header for new user.");
        }

        // Extract UUID from the Location header (e.g., /admin/realms/samskrtam/users/{uuid})
        String path = location.getPath();
        String userIdString = path.substring(path.lastIndexOf('/') + 1);
        UUID keycloakUserId = UUID.fromString(userIdString);

        log.debug("Keycloak user created: id={}", keycloakUserId);
        return keycloakUserId;
    }

    /**
     * Fetches user details from Keycloak by user ID (sub).
     *
     * @param userId The UUID of the user in Keycloak.
     * @return A Map containing user details from Keycloak.
     */
    public Map<String, Object> findUserById(UUID userId) {
        log.debug("Fetching user from Keycloak: userId={}", userId);
        try {
            return restClient.get()
                    .uri(adminUrl() + "/users/" + userId)
                    .header("Authorization", "Bearer " + tokenProvider.getToken())
                    .retrieve()
                    .body(Map.class); // Keycloak returns a JSON object for the user
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("User {} not found in Keycloak.", userId);
            return null; // Return null if user not found
        }
    }

    // PROFILE_UPDATED — синхронизация имени, фамилии, username
    private void updateKeycloakUser(String userId, Map<String, Object> payload) {
        log.debug("Updating Keycloak user profile: userId={}, payload={}", userId, payload);
        try {
            // 1. Get current full user representation from Keycloak
            Map<String, Object> currentUserRepresentation = findUserById(UUID.fromString(userId));
            if (currentUserRepresentation == null) {
                log.warn("Keycloak user {} not found for update, skipping profile update.", userId);
                return;
            }

            // 2. Update only the fields present in the payload
            if (payload.containsKey("firstName")) currentUserRepresentation.put("firstName", payload.get("firstName"));
            if (payload.containsKey("lastName"))  currentUserRepresentation.put("lastName",  payload.get("lastName"));
            if (payload.containsKey("username"))  currentUserRepresentation.put("username",  payload.get("username"));

            // 3. Send the modified full representation back to Keycloak using PUT
            restClient.put()
                    .uri(adminUrl() + "/users/" + userId)
                    .header("Authorization", "Bearer " + tokenProvider.getToken())
                    .body(currentUserRepresentation) // Send the full, modified representation
                    .retrieve()
                    .toBodilessEntity();

            log.debug("Keycloak user updated: userId={}, fields={}",
                    userId, payload.keySet());
        } catch (Exception e) {
            log.error("Failed to update Keycloak user {}: {}", userId, e.getMessage(), e);
            throw new OutboxProcessingException("Failed to update Keycloak user: " + userId, e);
        }
    }

    // USER_BLOCKED / USER_UNBLOCKED — включение/отключение аккаунта
    private void setEnabled(String userId, boolean enabled) {
        restClient.put()
                .uri(adminUrl() + "/users/" + userId)
                .header("Authorization", "Bearer " + tokenProvider.getToken())
                .body(Map.of("enabled", enabled))
                .retrieve()
                .toBodilessEntity();

        log.debug("Keycloak user enabled={}: userId={}", enabled, userId);
    }

    public void sendPasswordResetEmail(String userId) {
        log.debug("Sending password reset email for userId={}", userId);
        restClient.put()
                .uri(adminUrl() + "/users/" + userId + "/execute-actions-email")
                .header("Authorization", "Bearer " + tokenProvider.getToken())
                .body(List.of("UPDATE_PASSWORD")) // Action to execute
                .retrieve()
                .toBodilessEntity();
    }

    public void updateUserPassword(String userId, String newPassword) {
        log.debug("Updating password for userId={}", userId);
        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", newPassword,
                "temporary", false
        );
        restClient.put()
                .uri(adminUrl() + "/users/" + userId + "/reset-password")
                .header("Authorization", "Bearer " + tokenProvider.getToken())
                .body(credential)
                .retrieve()
                .toBodilessEntity();
    }

    private String adminUrl() {
        return keycloakUrl + "/admin/realms/" + realm;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parsePayload(String json) { // Changed to public for OutboxProcessor
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new OutboxProcessingException("Failed to parse payload: " + json, e);
        }
    }
}
