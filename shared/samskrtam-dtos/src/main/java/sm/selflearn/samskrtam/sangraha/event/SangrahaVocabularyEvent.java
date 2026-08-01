package sm.selflearn.samskrtam.sangraha.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Событие, отправляемое sangraha-service в content-service (синхронный REST-вызов,
 * бывший Kafka-топик sangraha-vocabulary-events, заменён по ADR-006).
 *
 * Consumer — content-service: строит VocabularyCategory (work/chapter/verse) и VocabularyWord.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SangrahaVocabularyEvent {

    @JsonProperty("verseId")
    private UUID verseId;

    @JsonProperty("workSlug")
    private String workSlug;

    @JsonProperty("workTitleRu")
    private String workTitleRu;

    @JsonProperty("workTitleEn")
    private String workTitleEn;

    @JsonProperty("chapterSlug")
    private String chapterSlug;

    @JsonProperty("chapterTitleRu")
    private String chapterTitleRu;

    @JsonProperty("chapterTitleEn")
    private String chapterTitleEn;

    @JsonProperty("verseOrderIndex")
    private int verseOrderIndex;

    @JsonProperty("words")
    private List<SangrahaVocabularyWord> words;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SangrahaVocabularyWord {

        @JsonProperty("verseWordId")
        private UUID verseWordId;

        @JsonProperty("wordIast")
        private String wordIast;

        @JsonProperty("wordDevanagari")
        private String wordDevanagari;

        @JsonProperty("stem")
        private String stem;

        @JsonProperty("root")
        private String root;

        @JsonProperty("gender")
        private String gender;

        @JsonProperty("translationRu")
        private String translationRu;

        @JsonProperty("translationEn")
        private String translationEn;

        @JsonProperty("explanationRu")
        private String explanationRu;

        @JsonProperty("explanationEn")
        private String explanationEn;
    }
}

