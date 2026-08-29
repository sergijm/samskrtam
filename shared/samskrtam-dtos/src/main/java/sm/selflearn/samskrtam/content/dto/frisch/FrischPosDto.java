package sm.selflearn.samskrtam.content.dto.frisch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrischPosDto(
        @JsonProperty("pos") String pos,
        @JsonProperty("qualifier") String qualifier
) {
}
