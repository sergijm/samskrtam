package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Ответ content-service на POST /content/internal/sangraha/vocabulary.
 * Возвращает vocabularyWordId для каждого слова из запроса, в том же порядке.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SangrahaVocabularyResponse {

    @JsonProperty("words")
    private List<WordEntry> words;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordEntry {

        @JsonProperty("wordIast")
        private String wordIast;

        @JsonProperty("stem")
        private String stem;

        @JsonProperty("vocabularyWordId")
        private UUID vocabularyWordId;
    }
}
