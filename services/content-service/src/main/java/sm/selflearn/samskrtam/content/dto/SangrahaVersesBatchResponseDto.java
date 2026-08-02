package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Ответ от POST {SANGRAHA_SERVICE_URL}/sangraha/internal/content/verses/batch
 * (sangraha-service.md §9). Клиентская копия sangraha DTO.
 */
@Value
@Builder
public class SangrahaVersesBatchResponseDto {

    List<VerseDto> verses;

    @JsonCreator
    public SangrahaVersesBatchResponseDto(@JsonProperty("verses") List<VerseDto> verses) {
        this.verses = verses;
    }

    @Value
    @Builder
    public static class VerseDto {
        UUID verseId;
        String workSlug;
        String textIast;
        String textDevanagari;
        String translationRu;
        String translationEn;
        String workTitleRu;
        String workTitleEn;
        String chapterTitleRu;
        String chapterTitleEn;
        int verseOrderIndex;

        @JsonCreator
        public VerseDto(
                @JsonProperty("verseId") UUID verseId,
                @JsonProperty("workSlug") String workSlug,
                @JsonProperty("textIast") String textIast,
                @JsonProperty("textDevanagari") String textDevanagari,
                @JsonProperty("translationRu") String translationRu,
                @JsonProperty("translationEn") String translationEn,
                @JsonProperty("workTitleRu") String workTitleRu,
                @JsonProperty("workTitleEn") String workTitleEn,
                @JsonProperty("chapterTitleRu") String chapterTitleRu,
                @JsonProperty("chapterTitleEn") String chapterTitleEn,
                @JsonProperty("verseOrderIndex") int verseOrderIndex) {
            this.verseId = verseId;
            this.workSlug = workSlug;
            this.textIast = textIast;
            this.textDevanagari = textDevanagari;
            this.translationRu = translationRu;
            this.translationEn = translationEn;
            this.workTitleRu = workTitleRu;
            this.workTitleEn = workTitleEn;
            this.chapterTitleRu = chapterTitleRu;
            this.chapterTitleEn = chapterTitleEn;
            this.verseOrderIndex = verseOrderIndex;
        }
    }
}