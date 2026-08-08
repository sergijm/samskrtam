package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID;

/**
 * One left-side row of a MATCHING question (questionType = "MATCHING"): a single word form
 * the user pairs with a case+number label. The correct case+number is carried for optional
 * frontend highlighting after an answer.
 *
 * @param id                stable row id (payload pair id) submitted back via
 *                          {@link MatchSubmissionDto#rowId()}
 * @param wordFormIast      the word form in IAST
 * @param wordFormDevanagari the word form in Devanagari
 * @param caseType          correct case (e.g. ACCUSATIVE)
 * @param numberType        correct number (e.g. SINGULAR)
 */
public record QuestionMatchRowDto(
        UUID id,
        String wordFormIast,
        String wordFormDevanagari,
        String caseType,
        String numberType
) {
}