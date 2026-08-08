package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID;

/**
 * One pool entry of a topic: quest-item id + its item-type code + progress tag.
 * Fetched from curriculum-service so quiz-service can run progress selection before composing.
 *
 * @param id          the quest item id
 * @param itemType    curriculum quest-item type code (e.g. DECLENSION_FORM_CHOICE)
 * @param progressTag progress grouping tag (caseType|numberType|gender for declensions, formIast for vocabulary)
 */
public record QuestPoolItemDto(
        UUID id,
        String itemType,
        String progressTag
) {
}