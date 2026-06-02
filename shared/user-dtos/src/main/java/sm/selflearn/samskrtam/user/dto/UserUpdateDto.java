package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UserUpdateDto {
    String locale; // Assuming locale is a String like "en" or "ru"
    String theme;  // Assuming theme is a String like "light" or "dark"
}
