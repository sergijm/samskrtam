package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID;

/**
 * One left-side row of a MATCHING question (questionType = "MATCHING"): a single word form
 * the user pairs with a label. For declension, the label is case+number; for conjugation,
 * it is person+number+voice.
 *
 * @param id                stable row id (payload pair id) submitted back via
 *                          {@link MatchSubmissionDto#rowId()}
 * @param wordFormIast      the word form in IAST
 * @param wordFormDevanagari the word form in Devanagari
 * @param caseType          correct case (e.g. ACCUSATIVE), null for conjugation
 * @param numberType        correct number (e.g. SINGULAR)
 * @param person            correct person (1/2/3), null for declension
 * @param voice             correct voice (PARASMAIPADA/ATMANEPADA), null for declension
 */
public record QuestionMatchRowDto(
        UUID id,
        String wordFormIast,
        String wordFormDevanagari,
        String caseType,
        String numberType,
        Integer person,
        String voice
) {
}