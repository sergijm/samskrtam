package sm.selflearn.samskrtam.common;

import java.time.Instant;

public record ErrorResponse(
    String  errorCode,
    String  message,
    String  traceId,
    Instant timestamp
) {
    public static ErrorResponse of(String errorCode, String message, String traceId) {
        return new ErrorResponse(errorCode, message, traceId, Instant.now());
    }

    // Overload for backward compatibility if traceId is not immediately available
    public static ErrorResponse of(String errorCode, String message) {
        return new ErrorResponse(errorCode, message, null, Instant.now());
    }
}
