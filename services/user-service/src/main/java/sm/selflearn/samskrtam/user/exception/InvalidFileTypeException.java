package sm.selflearn.samskrtam.user.exception;

public class InvalidFileTypeException extends RuntimeException {
    public InvalidFileTypeException(String contentType) {
        super("Invalid file type: " + contentType + ". Only image/jpeg, image/png, image/webp are allowed.");
    }
}
