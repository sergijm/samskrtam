package sm.selflearn.samskrtam.monierwilliams.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MwWordSearchDto {
    private String slp1Spelling;
    private String slp1Normalized;
    private String iastSpelling;
    private Double similarity;
}
