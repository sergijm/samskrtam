package sm.selflearn.samskrtam.curriculum.dto;

public record ErrorResponseDto(
        int status,
        String message,
        String details
) {
}
