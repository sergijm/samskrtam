package sm.selflearn.samskrtam.user.dto;

import sm.selflearn.samskrtam.user.model.UserRole;
import java.time.Instant;
import java.util.Set; // Import Set
import java.util.UUID;

public record PublicProfileResponse(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String avatarUrl,
        Set<UserRole> roles, // Changed from UserRole role to Set<UserRole> roles
        Instant createdAt
) {
    // Method from(UserProfile) removed, as it depends on UserProfile,
    // which should not be here.
    // Conversion logic will be implemented in user-service.
}
