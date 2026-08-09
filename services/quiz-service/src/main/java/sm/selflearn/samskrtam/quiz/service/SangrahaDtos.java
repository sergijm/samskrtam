package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
record SangrahaExamplesSearchResponse(
        @JsonProperty("groups") List<GroupDto> groups
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record GroupDto(
            @JsonProperty("caseType") String caseType,
            @JsonProperty("numberType") String numberType,
            @JsonProperty("verseIds") List<UUID> verseIds
    ) {}
}

@JsonIgnoreProperties(ignoreUnknown = true)
record SangrahaVersesBatchResponse(
        @JsonProperty("verses") List<VerseDto> verses
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record VerseDto(
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
            @JsonProperty("verseOrderIndex") int verseOrderIndex
    ) {}
}