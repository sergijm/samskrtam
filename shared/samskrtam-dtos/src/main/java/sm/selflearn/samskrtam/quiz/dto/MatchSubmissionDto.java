package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID;

/**
 * One user submission for a MATCHING question: which label (optionId) was paired with which
 * word form (rowId). The backend verifies the mapping against the materialized pairs.
 *
 * @param rowId    id of the word-form row ({@link QuestionMatchRowDto#id})
 * @param optionId id of the chosen case+number label option
 */
public record MatchSubmissionDto(
        UUID rowId,
        UUID optionId
) {
}