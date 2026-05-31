package sm.selflearn.samskrtam.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Глобальный фильтр Order 1.
 *
 * <p>Логирует входящий запрос и статус ответа.
 * Тела запроса/ответа не логируются (размер + sensitive данные).
 *
 * <p>Не логируются:
 * <ul>
 *   <li>userId в URL-параметрах (sensitive)
 *   <li>заголовки Authorization, X-User-* (токены и identity)
 *   <li>запросы к /actuator/health (слишком частые, засоряют логи)
 * </ul>
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final int ORDER = 1;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var request = exchange.getRequest();
        String path = request.getPath().value();

        // Не логируем health-check — слишком частые вызовы от k8s probes
        if (path.startsWith("/actuator/health")) {
            return chain.filter(exchange);
        }

        String requestId = request.getHeaders().getFirst("X-Request-Id");
        log.info("→ {} {} requestId={}", request.getMethod(), path, requestId);

        long startMs = System.currentTimeMillis();

        return chain.filter(exchange)
                .doFinally(signal -> {
                    ServerHttpResponse response = exchange.getResponse();
                    long durationMs = System.currentTimeMillis() - startMs;
                    int status = response.getStatusCode() != null
                            ? response.getStatusCode().value()
                            : 0;
                    log.info("← {} {} {} {}ms requestId={}",
                            request.getMethod(), path, status, durationMs, requestId);
                });
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
