package sm.selflearn.samskrtam.quiz.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Client mirror of curriculum-service {@code ProgressTagInfo}.
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