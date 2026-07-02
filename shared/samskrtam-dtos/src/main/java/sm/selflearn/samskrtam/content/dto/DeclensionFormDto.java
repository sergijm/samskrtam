package sm.selflearn.samskrtam.content.dto;

import com.fasterxml.jackson.annotation.JsonCreator; // Import JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty; // Import JsonProperty
import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;

import java.util.UUID;

@Value
public class DeclensionFormDto {
    UUID declensionStemId;
    CaseType caseType;
    NumberType numberType;
    String formIast;
    String formDevanagari;

    @JsonCreator
    @Builder // Keep @Builder for convenience in creating instances
    public DeclensionFormDto(
            @JsonProperty("declensionStemId") UUID declensionStemId,
            @JsonProperty("caseType") CaseType caseType,
            @JsonProperty("numberType") NumberType numberType,
            @JsonProperty("formIast") String formIast,
            @JsonProperty("formDevanagari") String formDevanagari) {
        this.declensionStemId = declensionStemId;
        this.caseType = caseType;
        this.numberType = numberType;
        this.formIast = formIast;
        this.formDevanagari = formDevanagari;
    }
}
