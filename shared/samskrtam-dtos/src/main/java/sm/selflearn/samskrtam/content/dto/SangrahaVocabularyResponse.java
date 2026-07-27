package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
import java.util.UUID;

/**
 * Ответ content-service на POST /content/internal/sangraha/vocabulary-quiz.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SangrahaVocabularyResponse {

    @JsonProperty("quizSlug")
    private String quizSlug;

    @JsonProperty("quizId")
    private UUID quizId;

    /** "CREATED" | "EXISTING" | null (null — не передавай в ответе вовсе, см. VocabularyQuizController в sangraha-service). */
    @JsonProperty("quizStatus")
    private String quizStatus;

    /** Маппинг verseWordId → vocabularyWordId для каждого отправленного слова. */
    @JsonProperty("wordMappings")
    private List<WordMapping> wordMappings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordMapping {
        @JsonProperty("verseWordId")
        private UUID verseWordId;

        @JsonProperty("vocabularyWordId")
        private UUID vocabularyWordId;
    }
}

