package sm.selflearn.samskrtam.quiz.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.error("=== ERROR DETAILS ===");
        log.error("Exception: {}", ex.getClass().getName());
        log.error("Message: {}", ex.getMessage());
        log.error("Request URI: {}", exchange.getRequest().getURI());
        log.error("HTTP Method: {}", exchange.getRequest().getMethod());

        HttpStatus status = resolveHttpStatus(ex);

        if (ex instanceof ServerWebInputException inputEx) {
            log.error("ServerWebInputException details:");
            log.error("  Reason: {}", inputEx.getReason());
            log.error("  Method parameter: {}", inputEx.getMethodParameter());
            if (inputEx.getCause() != null) {
                log.error("  Cause: {}", inputEx.getCause().getMessage());
            }
        }

        log.error("Stack trace:", ex);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(ex.getMessage().getBytes());

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private HttpStatus resolveHttpStatus(Throwable ex) {
        if (ex instanceof SamskrtamException se) {
            return switch (se.getErrorCode()) {
                case "SESSION_NOT_FOUND",
                     "STATUS_FILTER_POOL_EMPTY" -> HttpStatus.NOT_FOUND;
                case "ALREADY_ANSWERED" -> HttpStatus.CONFLICT;
                default -> HttpStatus.BAD_REQUEST;
            };
        }
        return HttpStatus.BAD_REQUEST;
    }
}
