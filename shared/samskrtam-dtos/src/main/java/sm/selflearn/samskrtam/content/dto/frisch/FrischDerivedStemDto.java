package sm.selflearn.samskrtam.content.dto.frisch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrischDerivedStemDto(
        @JsonProperty("derivation_type") String derivationType,
        @JsonProperty("form") String form,
        @JsonProperty("raw_tag") String rawTag
) {
}
