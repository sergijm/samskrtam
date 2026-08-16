package sm.selflearn.samskrtam.quest.syntax;

import sm.selflearn.samskrtam.quest.QuestItemPayload;

/**
 * Payload of a CASE_MEANING item. Carries the Sanskrit example and explanation
 * shown after the user answers.
 */
public record CaseMeaningPayload(
        String cardId,
        String caseType,
        String meaningType,
        String sanskritExample,
        String transliteration,
        String translation,
        String explanation
) implements QuestItemPayload {
}