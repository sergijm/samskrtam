package sm.selflearn.samskrtam.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatusCode; // Changed from HttpStatus
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class ServiceErrorLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            HttpStatusCode statusCode = exchange.getResponse().getStatusCode(); // Changed type to HttpStatusCode
            if (statusCode != null && (statusCode.is4xxClientError() || statusCode.is5xxServerError())) {
                log.error("Error response from service: method={}, path={}, status={}, headers={}",
                        exchange.getRequest().getMethod(),
                        exchange.getRequest().getPath(),
                        statusCode.value(),
                        exchange.getResponse().getHeaders());
            }
        }));
    }

    @Override
    public int getOrder() {
        // Выполняем этот фильтр после всех остальных, чтобы он мог логировать финальный статус ответа
        return Ordered.LOWEST_PRECEDENCE;
    }
}
