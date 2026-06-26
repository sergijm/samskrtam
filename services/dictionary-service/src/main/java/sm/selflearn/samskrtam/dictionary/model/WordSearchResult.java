package sm.selflearn.samskrtam.dictionary.model;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class WordSearchResult {
    private String slp1Spelling;
    private String slp1Normalized;
    private String iastSpelling;
    private double similarity;
    private DictionarySource source;
}