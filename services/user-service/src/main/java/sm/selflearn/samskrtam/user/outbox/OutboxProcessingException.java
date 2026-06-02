package sm.selflearn.samskrtam.user.outbox;

public class OutboxProcessingException extends RuntimeException {
    public OutboxProcessingException(String message) {
        super(message);
    }

    public OutboxProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
