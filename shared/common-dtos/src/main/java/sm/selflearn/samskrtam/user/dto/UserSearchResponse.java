package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class UserSearchResponse {
    UUID id;
    String username;
    String firstName;
    String lastName;
    String email;
}
