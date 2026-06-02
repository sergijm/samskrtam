package sm.selflearn.samskrtam.common;

public class SamskrtamException extends RuntimeException {
    private final String errorCode;

    public SamskrtamException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SamskrtamException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
