package sm.selflearn.samskrtam.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDto {
    private String id;
    private String username;
    private String email;
    private String role;
    private String locale;
}
