package sm.selflearn.samskrtam.auth.dto;

import lombok.Data;

@Data
public class RefreshRequest {
    private String refreshToken;
}
