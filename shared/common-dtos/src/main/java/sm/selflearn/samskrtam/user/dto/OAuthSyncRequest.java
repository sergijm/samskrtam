package sm.selflearn.samskrtam.user.dto;

import lombok.Builder;
import lombok.Data; // Изменено с @Value на @Data
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data // Изменено с @Value на @Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OAuthSyncRequest {
    String keycloakAccessToken;
    String provider;
}
