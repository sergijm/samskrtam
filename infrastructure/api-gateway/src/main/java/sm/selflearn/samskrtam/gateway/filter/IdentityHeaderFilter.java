package sm.selflearn.samskrtam.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Глобальный фильтр Order -2.
 *
 * <p>Извлекает данные из валидного JWT и добавляет downstream-заголовки:
 * <ul>
 *   <li>{@code X-User-Id} — UUID пользователя (JWT subject)
 *   <li>{@code X-User-Roles} — список ролей через запятую (из realm_access.roles)
 *   <li>{@code X-User-Locale} — ru | en (из claim "locale", по умолчанию "ru")
 * </ul>
 *
 * <p>Для публичных маршрутов (/api/v1/auth/**) principal отсутствует —
 * запрос пропускается без заголовков.
 */
@Slf4j
@Component
public class IdentityHeaderFilter implements GlobalFilter, Ordered {

    private static final int ORDER = -2;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    var jwt = auth.getToken();
                    String userId = jwt.getSubject();
                    String roles  = extractRoles(jwt.getClaimAsMap("realm_access")); // Changed to extractRoles
                    String locale = Optional.ofNullable(jwt.getClaimAsString("locale"))
                            .orElse("ru");

                    log.debug("IdentityHeaderFilter: userId={}, roles={}, locale={}",
                            userId, roles, locale);

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id",     userId)
                            .header("X-User-Roles",  roles) // Changed header name
                            .header("X-User-Locale", locale)
                            .build();

                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                // Нет principal — публичный маршрут, пропускаем без заголовков
                .switchIfEmpty(chain.filter(exchange));
    }

    /**
     * Извлекает все роли из realm_access.roles и возвращает их как строку, разделенную запятыми.
     */
    private String extractRoles(Map<String, Object> realmAccess) {
        if (realmAccess == null) {
            return "STUDENT"; // Default role if no realm_access
        }
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) realmAccess.getOrDefault("roles", List.of("STUDENT")); // Default to STUDENT
        return roles.stream()
                .map(String::toUpperCase) // Ensure roles are uppercase
                .collect(Collectors.joining(","));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
