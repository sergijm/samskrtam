package sm.selflearn.samskrtam.content.dto.frisch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrischVerbFormDto(
        @JsonProperty("form_type") String formType,
        @JsonProperty("tense") String tense,
        @JsonProperty("mood") String mood,
        @JsonProperty("voice") String voice,
        @JsonProperty("person") String person,
        @JsonProperty("number") String number,
        @JsonProperty("vedic") Boolean vedic,
        @JsonProperty("form") String form,
        @JsonProperty("raw_tag") String rawTag
) {
}
