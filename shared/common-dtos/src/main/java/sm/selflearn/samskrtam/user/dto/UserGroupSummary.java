package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class UserGroupSummary {
    UUID groupId;
    String groupName;
    String groupRole; // e.g., "CURATOR", "MEMBER"
    Instant joinedAt; // Assuming joinedAt is part of the summary
}
