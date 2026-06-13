package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Set;
import java.util.UUID;

@Value
@Builder
public class UserProfileResponse {
    UUID id;
    String username;
    String email;
    String firstName;
    String lastName;
    String avatarUrl;
    Set<String> roles;
    String theme;
    String locale;
    Integer quizSize; // New field
}
