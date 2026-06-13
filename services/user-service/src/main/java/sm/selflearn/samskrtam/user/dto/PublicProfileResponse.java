package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.user.model.UserRole;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Value
@Builder
public class PublicProfileResponse {
    UUID id;
    String username;
    String firstName;
    String lastName;
    String avatarUrl;
    Set<UserRole> roles;
    Instant createdAt;
}
