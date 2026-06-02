package sm.selflearn.samskrtam.common;

import java.time.Instant;

public record ErrorResponse(
    String  errorCode,
    String  message,
    String  traceId,
    Instant timestamp
) {
    public static ErrorResponse of(String errorCode, String message) {
        // MDC.get("traceId") is not available in a shared DTO module without Spring context
        // For now, we'll use a placeholder or null. The actual traceId injection happens at the service layer.
        return new ErrorResponse(errorCode, message, null, Instant.now());
    }
}
