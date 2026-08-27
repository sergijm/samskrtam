package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.morphology.Pada;

import java.util.List;

/**
 * The present-tense paradigm of one verb lemma: lemma metadata plus its
 * example-sentence cells (person x number).
 */
@Value
@Builder
public class ConjugationParadigmDto {
    String lemmaIast;
    String lemmaDevanagari;
    String meaningRu;
    Pada voice;
    List<ConjugationFormDto> forms;

    @JsonCreator
    public ConjugationParadigmDto(
            @JsonProperty("lemmaIast") String lemmaIast,
            @JsonProperty("lemmaDevanagari") String lemmaDevanagari,
            @JsonProperty("meaningRu") String meaningRu,
            @JsonProperty("voice") Pada voice,
            @JsonProperty("forms") List<ConjugationFormDto> forms) {
        this.lemmaIast = lemmaIast;
        this.lemmaDevanagari = lemmaDevanagari;
        this.meaningRu = meaningRu;
        this.voice = voice;
        this.forms = forms;
    }
}