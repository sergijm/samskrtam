package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.morphology.NumberType;

/**
 * One conjugation paradigm cell: the example sentence for a (person, number) of
 * a verb lemma in the present tense.
 */
@Value
public class ConjugationFormDto {
    int person;
    NumberType numberType;
    String sentenceIast;
    String sentenceDevanagari;
    String translationRu;

    @JsonCreator
    @Builder
    public ConjugationFormDto(
            @JsonProperty("person") int person,
            @JsonProperty("numberType") NumberType numberType,
            @JsonProperty("sentenceIast") String sentenceIast,
            @JsonProperty("sentenceDevanagari") String sentenceDevanagari,
            @JsonProperty("translationRu") String translationRu) {
        this.person = person;
        this.numberType = numberType;
        this.sentenceIast = sentenceIast;
        this.sentenceDevanagari = sentenceDevanagari;
        this.translationRu = translationRu;
    }
}