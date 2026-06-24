package sm.selflearn.samskrtam.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(0)
@Slf4j
public class JwtParsingGlobalFilter implements GlobalFilter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Map<String, Object> claims = parseJwtWithoutVerification(token);
                String userId = (String) claims.get("sub");
                String email = (String) claims.get("email");
                String username = (String) claims.get("preferred_username");

                if (userId != null) {
                    log.debug("Parsed JWT for user: {}", userId);

                    // Создаем новый запрос с добавленными заголовками
                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header("X-User-Id", userId)
                            .header("X-User-Email", email != null ? email : "")
                            .header("X-User-Username", username != null ? username : "")
                            .header("X-User-Roles", extractRoles(claims))
                            .build();

                    return chain.filter(exchange.mutate()
                            .request(mutatedRequest)
                            .build());
                }
            } catch (Exception e) {
                log.warn("Failed to parse JWT token: {}", e.getMessage());
            }
        }

        return chain.filter(exchange);
    }

    private Map<String, Object> parseJwtWithoutVerification(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid JWT token");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            @SuppressWarnings("unchecked")
            Map<String, Object> claims = objectMapper.readValue(payload, HashMap.class);

            return claims;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JWT payload", e);
        }
    }

    private String extractRoles(Map<String, Object> claims) {
        Object rolesObj = claims.get("roles");
        if (rolesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) rolesObj;
            return String.join(",", roles);
        }
        return "";
    }

}