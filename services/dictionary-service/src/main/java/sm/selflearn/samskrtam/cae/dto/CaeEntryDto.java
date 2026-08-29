package sm.selflearn.samskrtam.cae.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CaeEntryDto {
    private Long id;
    private Integer page;
    private Integer homonymNum;
    private String entryVariant;
    private String headwordPlain;
    private String headwordAccented;
    private String rawText;
    private String cleanText;
    private String gloss;
    private String grammarPos;
}
