package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class Group {
    UUID id;
    String name;
    UUID curatorId;
    String curatorName;
    int memberCount;
    Instant createdAt;
}
