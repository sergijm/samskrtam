package sm.selflearn.samskrtam.apte.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApteEntryDto {
    private Long id;
    private String headwordDevanagari;
    private String bodyText;
    private String rawMarkup;
    private Integer homonymNum;
}
