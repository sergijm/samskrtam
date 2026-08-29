package sm.selflearn.samskrtam.user.dto;

import java.time.Instant;
import java.util.UUID;

public record BlockUserResponse(
    UUID id,
    boolean blocked,
    Instant updatedAt
) {}
