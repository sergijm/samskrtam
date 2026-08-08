package sm.selflearn.samskrtam.quiz.dto;

/**
 * Client-side mirror of curriculum-service {@code ComposedQuizItemDto}: a materialized
 * quest item positioned in the session sequence.
 *
 * @param questionNumber 1-based position in the final sequence
 * @param topicCode      topic the item belongs to
 * @param item           the rendered quest item (prompt + correctAnswer + distractors + payload)
 * @param progressTag    progress grouping tag (caseType|numberType|gender for declensions, formIast for vocabulary)
 */
public record ComposedQuestionDto(
        int questionNumber,
        String topicCode,
        QuestItemDto item,
        String progressTag
) {
}