package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupMember {
    UUID userId;
    String username;
    String email;
    String groupRole; // e.g., "CURATOR", "MEMBER"
    Instant joinedAt;
}
