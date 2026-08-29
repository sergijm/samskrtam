package sm.selflearn.samskrtam.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @Size(max = 100)
        String firstName,

        @Size(max = 100)
        String lastName
) {}
