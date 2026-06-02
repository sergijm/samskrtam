package sm.selflearn.samskrtam.user.dto;

import sm.selflearn.samskrtam.user.model.UserRole; // Исправлен импорт на sm.selflearn.samskrtam.user.model.UserRole
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
    // Методы from(UserProfile) и toPublicProfileResponse() удалены,
    // так как они зависят от UserProfile и PublicProfileResponse,
    // которые не должны быть здесь или должны быть чистыми DTO.
    // Логика конвертации будет реализована в user-service.
}
