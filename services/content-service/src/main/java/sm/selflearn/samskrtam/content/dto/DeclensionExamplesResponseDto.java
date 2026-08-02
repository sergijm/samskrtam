package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Ответ GET /content/public/lessons/{slug}/examples
 * (content-service.md §12, declension-examples.md).
 * Группы по каждой ячейке (caseType, numberType) парадигмы с примерами-цитатами.
 */
@Value
@Builder
public class DeclensionExamplesResponseDto {

    List<GroupDto> groups;

    @JsonCreator
    public DeclensionExamplesResponseDto(@JsonProperty("groups") List<GroupDto> groups) {
        this.groups = groups;
    }

    @Value
    @Builder
    public static class GroupDto {
        String caseType;
        String numberType;
        List<ExampleDto> examples;

        @JsonCreator
        public GroupDto(
                @JsonProperty("caseType") String caseType,
                @JsonProperty("numberType") String numberType,
                @JsonProperty("examples") List<ExampleDto> examples) {
            this.caseType = caseType;
            this.numberType = numberType;
            this.examples = examples;
        }
    }

    @Value
    @Builder
    public static class ExampleDto {
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
        public ExampleDto(
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