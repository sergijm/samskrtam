package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.List; // For GroupMember
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GroupDetail {
    UUID id;
    String name;
    UUID curatorId;
    String curatorName;
    int memberCount;
    Instant createdAt;
    List<GroupMember> members; // List of GroupMember DTOs
}
