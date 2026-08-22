package sm.selflearn.samskrtam.content.dto.frisch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrischSenseDto(
        @JsonProperty("cs") String cs,
        @JsonProperty("en") String en,
        @JsonProperty("ru") String ru,
        @JsonProperty("genders") List<String> genders,
        @JsonProperty("number_note") String numberNote,
        @JsonProperty("is_proper_noun") Boolean isProperNoun
) {
}
