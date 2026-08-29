package sm.selflearn.samskrtam.content.dto.frisch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrischEntryDto(
        @JsonProperty("pos") List<FrischPosDto> pos,
        @JsonProperty("senses") List<FrischSenseDto> senses,
        @JsonProperty("genders") List<FrischGenderDto> genders,
        @JsonProperty("is_root") Boolean isRoot,
        @JsonProperty("entry_id") Integer entryId,
        @JsonProperty("gloss_cs") String glossCs,
        @JsonProperty("gloss_en") String glossEn,
        @JsonProperty("gloss_ru") String glossRu,
        @JsonProperty("lemma_iast") String lemmaIast,
        @JsonProperty("verb_class") Integer verbClass,
        @JsonProperty("verb_forms") List<FrischVerbFormDto> verbForms,
        @JsonProperty("grammar_note") String grammarNote,
        @JsonProperty("parent_lemma") String parentLemma,
        @JsonProperty("raw_headline") String rawHeadline,
        @JsonProperty("derived_stems") List<FrischDerivedStemDto> derivedStems,
        @JsonProperty("homonym_index") Integer homonymIndex,
        @JsonProperty("related_forms") List<FrischRelatedFormDto> relatedForms,
        @JsonProperty("is_related_form") Boolean isRelatedForm,
        @JsonProperty("parent_entry_id") Integer parentEntryId,
        @JsonProperty("cross_references") List<FrischCrossReferenceDto> crossReferences
) {
}
