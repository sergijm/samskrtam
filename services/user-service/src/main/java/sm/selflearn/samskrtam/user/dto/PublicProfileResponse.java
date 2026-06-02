package sm.selflearn.samskrtam.user.dto;

import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.model.UserRole;

import java.time.Instant;
import java.util.UUID;

public record PublicProfileResponse(
        UUID id,
        String username,
        String firstName,
        String lastName,
        String avatarUrl,
        UserRole role,
        Instant createdAt
) {
    public static PublicProfileResponse from(UserProfile userProfile) {
        return new PublicProfileResponse(
                userProfile.getId(),
                userProfile.getUsername(),
                userProfile.getFirstName(),
                userProfile.getLastName(),
                userProfile.getAvatarUrl(),
                userProfile.getRole(),
                userProfile.getCreatedAt()
        );
    }
}
