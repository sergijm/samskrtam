package sm.selflearn.samskrtam.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Глобальный фильтр Order 0.
 *
 * <p>Добавляет заголовок {@code X-Request-Id} ко всем запросам.
 * Если клиент уже прислал X-Request-Id — переиспользуем его (идемпотентность).
 * Downstream сервисы логируют requestId для корреляции запросов.
 */
@Slf4j
@Component
public class RequestIdFilter implements GlobalFilter, Ordered {

    private static final int    ORDER           = 0;
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String existingId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        String requestId  = (existingId != null && !existingId.isBlank())
                ? existingId
                : UUID.randomUUID().toString();

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header(REQUEST_ID_HEADER, requestId)
                .build();

        // Continue the filter chain with the mutated request.
        // Then, after the chain has processed, add the X-Request-Id to the response headers.
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .then(Mono.fromRunnable(() -> {
                    ServerHttpResponse response = exchange.getResponse();
                    //response.getHeaders().add(REQUEST_ID_HEADER, requestId);
                    log.debug("RequestIdFilter: requestId={}, path={} (response header added)",
                            requestId, exchange.getRequest().getPath());
                }));
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
