package sm.selflearn.samskrtam.gateway.exception;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Глобальный обработчик исключений для Gateway.
 *
 * <p>Гарантирует что 401, 403, 404 и 500 ошибки возвращаются как JSON,
 * а не как HTML страница Netty по умолчанию.
 *
 * <p>Acceptance criteria: "401 и 403 возвращают JSON, не HTML".
 */
@Slf4j
@Component
@Order(-1) // Выше DefaultErrorWebExceptionHandler Spring Boot
@RequiredArgsConstructor // Для инъекции ServerCodecConfigurer
public class GlobalExceptionHandler implements ErrorWebExceptionHandler, ServerResponse.Context {

    private final ServerCodecConfigurer serverCodecConfigurer;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Resolve status and message
        HttpStatus status = resolveStatus(ex);
        String message    = resolveMessage(ex, status);

        if (status.is5xxServerError()) {
            log.error("Gateway error: path={}, error={}",
                    exchange.getRequest().getPath(), ex.getMessage(), ex);
        } else {
            log.debug("Gateway client error: path={}, status={}, message={}",
                    exchange.getRequest().getPath(), status.value(), message);
        }

        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        String jsonBody = buildJsonError(status, message, requestId);

        // Use ServerResponse to build the error response and write it to the exchange
        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonBody)
                .flatMap(response -> response.writeTo(exchange, this)); // Pass 'this' as ServerResponse.Context
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException rse) {
            return HttpStatus.valueOf(rse.getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        if (ex instanceof ResponseStatusException rse
                && rse.getReason() != null && !rse.getReason().isBlank()) {
            return rse.getReason();
        }
        return status.getReasonPhrase();
    }

    private String buildJsonError(HttpStatus status, String message, String requestId) {
        // Простой JSON без Jackson — избегаем зависимости на ObjectMapper в Gateway
        return String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"requestId\":\"%s\"}",
                Instant.now(),
                status.value(),
                escapeJson(message),
                requestId != null ? requestId : ""
        );
    }

    private String escapeJson(String s) {
        return s.replace("\"", "\\\"");
    }

    // Implement ServerResponse.Context methods
    @Override
    public List<HttpMessageWriter<?>> messageWriters() {
        return serverCodecConfigurer.getWriters();
    }

    @Override
    public List<ViewResolver> viewResolvers() {
        return Collections.emptyList(); // Not needed for REST API
    }
}
