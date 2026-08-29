package sm.selflearn.samskrtam.mw.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MwEntryDto {
    private Long id;
    private String entryId;
    private String key1;
    private String key2;
    private String homonym;
    private String entryNo;
    private String pageCol;
    private String headwordDisplay;
    private String body;
    private String grammarJson;
    private String cleanText;
    private String html;
    private String headwordIast;
    private String pageRefsHtml;
}
