package sm.selflearn.samskrtam.common;

/**
 * A generic error response DTO.
 */
public record ErrorResponse(String message, java.time.Instant timestamp) {}
