package sm.selflearn.samskrtam.curriculum.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO for a single Emeneau sandhi rule from the static JSON resource.
 * Mirrors the structure of emenau-sandhi-rules.json rules[] entries.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SandhiRuleDto(
        int number,
        @JsonProperty("section") String section,
        String applicability,
        String text,
        String example,
        String reference,
        @JsonAlias("depends_on") List<Integer> dependsOn,
        @JsonAlias("depends_on_note") String dependsOnNote
) {}