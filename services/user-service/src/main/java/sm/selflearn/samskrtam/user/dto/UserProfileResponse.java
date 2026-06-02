package sm.selflearn.samskrtam.user.dto;

import sm.selflearn.samskrtam.user.model.UserRole;
import sm.selflearn.samskrtam.user.model.UserProfile;

import java.time.Instant;
import java.util.UUID;

public record UserProfileResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String avatarUrl,
        UserRole role,
        boolean blocked,
        Instant createdAt
) {
    public static UserProfileResponse from(UserProfile userProfile) {
        return new UserProfileResponse(
                userProfile.getId(),
                userProfile.getUsername(),
                userProfile.getEmail(),
                userProfile.getFirstName(),
                userProfile.getLastName(),
                userProfile.getAvatarUrl(),
                userProfile.getRole(),
                userProfile.isBlocked(),
                userProfile.getCreatedAt()
        );
    }

    public PublicProfileResponse toPublicProfileResponse() {
        return new PublicProfileResponse(
                this.id,
                this.username,
                this.firstName,
                this.lastName,
                this.avatarUrl,
                this.role,
                this.createdAt
        );
    }
}
