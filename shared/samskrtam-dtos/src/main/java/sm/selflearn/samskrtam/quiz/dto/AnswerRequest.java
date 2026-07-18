package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class AnswerRequest {
    UUID questionId;

    /** Single selected option ID (for single-select questions, e.g. FORM_BY_CASE). */
    UUID selectedOptionId;

    /** Multiple selected option IDs (for multi-select questions, e.g. CASE_BY_FORM). */
    List<UUID> selectedOptionIds;
    String selectedFormIast;
    int responseTimeMs;
}

