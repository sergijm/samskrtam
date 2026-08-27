package sm.selflearn.samskrtam.user.dto;

import sm.selflearn.samskrtam.user.model.UserRole;
import java.time.Instant;
import java.util.Set; // Import Set
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        Set<UserRole> roles, // Changed from UserRole role to Set<UserRole> roles
        boolean blocked,
        Instant createdAt
) {
    // Methods from(UserProfile) and toPublicProfileResponse() removed,
    // as they depend on UserProfile and PublicProfileResponse,
    // which should not be here or should be pure DTOs.
    // Conversion logic will be implemented in user-service.
}
