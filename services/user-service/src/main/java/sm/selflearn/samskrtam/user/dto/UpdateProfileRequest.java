package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UpdateProfileRequest {
    String username;
    String firstName;
    String lastName;
    Integer quizSize; // New field
}
