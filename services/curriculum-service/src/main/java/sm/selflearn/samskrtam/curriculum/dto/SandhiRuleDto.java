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
        @JsonAlias("supersedes") List<Integer> supersedes,
        @JsonAlias("default_for") List<Integer> defaultFor,
        @JsonAlias("applies_with") List<Integer> appliesWith,
        List<String> category
) {}