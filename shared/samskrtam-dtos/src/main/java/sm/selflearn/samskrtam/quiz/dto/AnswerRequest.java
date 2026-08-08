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

    /** For CASE_BY_FORM/ENDING_MATCH: caseType of the selected option. */
    String selectedCaseType;

    /** For CASE_BY_FORM/ENDING_MATCH: numberType of the selected option. */
    String selectedNumberType;

    /** For CASE_BY_FORM/ENDING_MATCH: gender of the selected option. */
    String selectedGender;

    /**
     * Submissions for a MATCHING question (questionType = "MATCHING"): each word-form row
     * paired with a chosen case+number label. Null/empty for non-matching questions.
     */
    List<MatchSubmissionDto> matchSubmissions;
}

