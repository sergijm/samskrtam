package sm.selflearn.samskrtam.curriculum.questsession.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemDto;

/**
 * One question of a composed session sequence. Wraps the ready-made quest item
 * (prompt + correctAnswer + distractors + payload are materialized by
 * curriculum-service, see curriculum-quest-items.md) and adds session-level
 * positioning.
 *
 * @param questionNumber 1-based position in the final (randomized) sequence
 * @param topicCode      topic the item belongs to
 * @param item           the rendered quest item with its options
 * @param progressTag    progress grouping tag (caseType|numberType|gender for declensions, formIast for vocabulary)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ComposedQuizItemDto(
        int questionNumber,
        String topicCode,
        QuestItemDto item,
        String progressTag
) {
}