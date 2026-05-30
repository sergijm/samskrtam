package sm.selflearn.samskrtam.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.auth.client.KeycloakClient;
import sm.selflearn.samskrtam.auth.dto.AuthResponse;
import sm.selflearn.samskrtam.auth.dto.UserDto;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final KeycloakClient keycloakClient;
    private final ObjectMapper objectMapper;

    @Value("${keycloak.client-id}")
    private String clientId;
    @Value("${keycloak.client-secret}")
    private String clientSecret;

    public AuthResponse login(String email, String password) throws IOException {
        Map<String, Object> tokenResponse = keycloakClient.login(clientId, clientSecret, email, password);
        return buildAuthResponse(tokenResponse);
    }

    public AuthResponse exchangeCode(String code, String redirectUri) throws IOException {
        Map<String, Object> tokenResponse = keycloakClient.exchangeCode(clientId, clientSecret, code, redirectUri);
        return buildAuthResponse(tokenResponse);
    }

    public AuthResponse refresh(String refreshToken) throws IOException {
        Map<String, Object> tokenResponse = keycloakClient.refresh(clientId, clientSecret, refreshToken);
        return buildAuthResponse(tokenResponse);
    }

    private AuthResponse buildAuthResponse(Map<String, Object> tokenResponse) throws IOException {
        String accessToken = (String) tokenResponse.get("access_token");
        UserDto user = parseToken(accessToken);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken((String) tokenResponse.get("refresh_token"))
                .expiresIn((Integer) tokenResponse.get("expires_in"))
                .user(user)
                .build();
    }

    private UserDto parseToken(String token) throws IOException {
        String[] parts = token.split("\\.");
        byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
        Map<String, Object> claims = objectMapper.readValue(payloadBytes, Map.class);
        Map<String, Object> realmAccess = (Map<String, Object>) claims.get("realm_access");
        List<String> roles = (List<String>) realmAccess.get("roles");

        return UserDto.builder()
                .id((String) claims.get("sub"))
                .username((String) claims.get("preferred_username"))
                .email((String) claims.get("email"))
                .role(roles.isEmpty() ? "STUDENT" : roles.get(0))
                .locale((String) claims.get("locale"))
                .build();
    }
}
