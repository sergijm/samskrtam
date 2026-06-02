package sm.selflearn.samskrtam.user.exception;

public class UserPasswordUpdateException extends RuntimeException {
    public UserPasswordUpdateException(String message) {
        super(message);
    }

    public UserPasswordUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
