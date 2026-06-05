package sm.selflearn.samskrtam.user.exception;

import java.util.UUID;

public class GroupNotFoundException extends RuntimeException {
    public GroupNotFoundException(UUID groupId) {
        super("Group not found with ID: " + groupId);
    }

    public GroupNotFoundException(String message) {
        super(message);
    }

    public GroupNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
