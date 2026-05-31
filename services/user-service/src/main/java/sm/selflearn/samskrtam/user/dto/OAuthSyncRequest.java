package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class OAuthSyncRequest {
    String keycloakToken;
    String provider;
}
