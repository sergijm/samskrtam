package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.VowelType;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class DeclensionParadigmDto {
    UUID stemId;
    String stemIast;
    String stemDevanagari;
    String translationRu;
    String translationEn;
    Gender gender;
    VowelType vowelType;
    List<DeclensionFormDto> forms;

    @JsonCreator
    public DeclensionParadigmDto(
            @JsonProperty("stemId") UUID stemId,
            @JsonProperty("stemIast") String stemIast,
            @JsonProperty("stemDevanagari") String stemDevanagari,
            @JsonProperty("translationRu") String translationRu,
            @JsonProperty("translationEn") String translationEn,
            @JsonProperty("gender") Gender gender,
            @JsonProperty("vowelType") VowelType vowelType,
            @JsonProperty("forms") List<DeclensionFormDto> forms) {
        this.stemId = stemId;
        this.stemIast = stemIast;
        this.stemDevanagari = stemDevanagari;
        this.translationRu = translationRu;
        this.translationEn = translationEn;
        this.gender = gender;
        this.vowelType = vowelType;
        this.forms = forms;
    }
}
