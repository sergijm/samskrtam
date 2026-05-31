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

/**
 * Глобальный фильтр Order -2.
 *
 * <p>Извлекает данные из валидного JWT и добавляет downstream-заголовки:
 * <ul>
 *   <li>{@code X-User-Id} — UUID пользователя (JWT subject)
 *   <li>{@code X-User-Role} — ADMIN | STUDENT (из realm_access.roles)
 *   <li>{@code X-User-Locale} — ru | en (из claim "locale", по умолчанию "ru")
 * </ul>
 *
 * <p>Для публичных маршрутов (/api/v1/auth/**) principal отсутствует —
 * запрос пропускается без заголовков.
 *
 * <p>Роль определяется явным поиском "ADMIN" в списке ролей,
 * а не по индексу — пользователь может иметь несколько ролей.
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
                    String role   = extractRole(jwt.getClaimAsMap("realm_access"));
                    String locale = Optional.ofNullable(jwt.getClaimAsString("locale"))
                            .orElse("ru");

                    log.debug("IdentityHeaderFilter: userId={}, role={}, locale={}",
                            userId, role, locale);

                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                            .header("X-User-Id",     userId)
                            .header("X-User-Role",   role)
                            .header("X-User-Locale", locale)
                            .build();

                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                // Нет principal — публичный маршрут, пропускаем без заголовков
                .switchIfEmpty(chain.filter(exchange));
    }

    /**
     * Ищет роль "ADMIN" явно в списке realm_access.roles.
     * Не полагается на порядок ролей — у пользователя может быть несколько.
     */
    private String extractRole(Map<String, Object> realmAccess) {
        if (realmAccess == null) {
            return "STUDENT";
        }
        @SuppressWarnings("unchecked")
        List<Object> roles = (List<Object>) realmAccess.getOrDefault("roles", List.of());
        boolean isAdmin = roles.stream()
                .anyMatch(r -> "ADMIN".equals(r.toString()));
        return isAdmin ? "ADMIN" : "STUDENT";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
