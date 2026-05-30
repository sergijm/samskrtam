package sm.selflearn.samskrtam.auth.dto;

import lombok.Data;

@Data
public class CallbackRequest {
    private String code;
    private String redirectUri;
}
