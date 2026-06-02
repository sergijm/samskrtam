package sm.selflearn.samskrtam.user.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_profiles", schema = "users")
@Data
@EqualsAndHashCode(of = "id")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfile {

    @Id
    private UUID id;                      // совпадает с Keycloak sub

    @Column(unique = true, nullable = false)
    private String username;

    @Column(unique = true, nullable = false)
    private String email;                 // только для чтения после регистрации

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "avatar_url")
    private String avatarUrl;             // публичный URL в MinIO (avatars/)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;                // STUDENT, ADMIN

    @Column(name = "blocked", nullable = false)
    private boolean blocked = false;      // дублируется из Keycloak для поиска/фильтрации

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID(); // Assign a UUID if not already set (e.g., from Keycloak sub)
        }
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
