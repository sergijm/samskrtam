package sm.selflearn.samskrtam.quiz;

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
import sm.selflearn.samskrtam.common.ErrorResponse;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        // Детальное логирование ошибки
        log.error("=== ERROR DETAILS ===");
        log.error("Exception: {}", ex.getClass().getName());
        log.error("Message: {}", ex.getMessage());
        log.error("Request URI: {}", exchange.getRequest().getURI());
        log.error("HTTP Method: {}", exchange.getRequest().getMethod());
        log.error("Headers: {}", exchange.getRequest().getHeaders());

        // Логируем параметры запроса
        exchange.getRequest().getQueryParams().forEach((key, value) ->
                log.error("Query param - {}: {}", key, value));

        // Логируем все атрибуты
        exchange.getAttributes().forEach((key, value) ->
                log.debug("Attribute - {}: {}", key, value));

        // Полный стектрейс
        log.error("Stack trace:", ex);

        // Для ServerWebInputException показываем детали
        if (ex instanceof ServerWebInputException) {
            ServerWebInputException inputEx = (ServerWebInputException) ex;
            log.error("ServerWebInputException details:");
            log.error("  Reason: {}", inputEx.getReason());
            log.error("  Method parameter: {}", inputEx.getMethodParameter());

            // Если есть причина, логируем её
            if (inputEx.getCause() != null) {
                log.error("  Cause: {}", inputEx.getCause().getMessage());
            }
        }


        exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(ex.getMessage().getBytes());

        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}