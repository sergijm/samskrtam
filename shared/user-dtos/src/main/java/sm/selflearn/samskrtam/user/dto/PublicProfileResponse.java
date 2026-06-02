package sm.selflearn.samskrtam.user.dto;

import sm.selflearn.samskrtam.user.model.UserRole; // Исправлен импорт на sm.selflearn.samskrtam.user.model.UserRole
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
    // Метод from(UserProfile) удален, так как он зависит от UserProfile,
    // которая не должна быть здесь.
    // Логика конвертации будет реализована в user-service.
}
