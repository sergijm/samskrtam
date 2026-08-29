package sm.selflearn.samskrtam.search.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class LemmaSearchResult {
    private Long lemmaId;
    private String dictionaryCode;
    private String k1Slp1;
    private String k2Original;
    private String headwordDisplay;
    private String lemmaDevanagari;
    private String k1Iast;
    private String path;
    private double score;
    private Map<String, Object> notes;
    private Map<String, long[]> entries;
}
