package sm.selflearn.samskrtam.content.dto.frisch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrischGenderDto(
        @JsonProperty("gender") String gender,
        @JsonProperty("stem_suffix") String stemSuffix
) {
}
