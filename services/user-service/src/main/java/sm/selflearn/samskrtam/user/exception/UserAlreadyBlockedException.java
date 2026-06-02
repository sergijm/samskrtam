package sm.selflearn.samskrtam.user.exception;

import java.util.UUID;

public class UserAlreadyBlockedException extends RuntimeException {
    public UserAlreadyBlockedException(UUID userId) {
        super("User with ID " + userId + " is already blocked or not blocked as expected.");
    }

    public UserAlreadyBlockedException(String message) {
        super(message);
    }
}
