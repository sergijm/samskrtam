package sm.selflearn.samskrtam.user.dto;

import jakarta.validation.constraints.NotBlank;

public record AvatarConfirmRequest(
        @NotBlank
        String objectKey
) {}
