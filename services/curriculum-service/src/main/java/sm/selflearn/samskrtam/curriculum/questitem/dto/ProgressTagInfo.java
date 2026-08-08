package sm.selflearn.samskrtam.curriculum.questitem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Metadata for one progress cell of a topic lesson. Keyed by progressTag
 * ({@code caseType|numberType|gender} for declensions, {@code formIast} for vocabulary).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProgressTagInfo(
        String itemType,
        String gender,
        String caseType,
        String numberType,
        String formIast
) {
}