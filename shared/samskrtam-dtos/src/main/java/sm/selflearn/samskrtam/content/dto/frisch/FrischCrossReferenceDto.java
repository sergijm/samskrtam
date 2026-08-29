package sm.selflearn.samskrtam.content.dto.frisch;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FrischCrossReferenceDto(
        @JsonProperty("kind") String kind,
        @JsonProperty("target_raw") String targetRaw,
        @JsonProperty("target_entry_id") Integer targetEntryId,
        @JsonProperty("target_lemma") String targetLemma
) {
}
