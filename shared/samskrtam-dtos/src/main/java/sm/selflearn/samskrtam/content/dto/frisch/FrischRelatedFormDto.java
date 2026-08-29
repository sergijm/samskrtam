package sm.selflearn.samskrtam.content.dto.frisch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrischRelatedFormDto(
        @JsonProperty("derivation_type") String derivationType,
        @JsonProperty("preverb") String preverb,
        @JsonProperty("surface_form") String surfaceForm,
        @JsonProperty("case_government") String caseGovernment,
        @JsonProperty("entry_id") Integer entryId,
        @JsonProperty("lemma_iast") String lemmaIast
) {
}
