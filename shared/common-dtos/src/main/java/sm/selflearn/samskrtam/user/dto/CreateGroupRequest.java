package sm.selflearn.samskrtam.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CreateGroupRequest {
    @NotBlank
    @Size(min = 3, max = 100)
    String name;
}
