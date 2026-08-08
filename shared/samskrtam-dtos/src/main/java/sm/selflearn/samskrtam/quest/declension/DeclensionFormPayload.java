package sm.selflearn.samskrtam.quest.declension;

import sm.selflearn.samskrtam.quest.QuestItemPayload;

/**
 * Shared payload for DECLENSION_FORM and DECLENSION_FORM_CHOICE.
 * See curriculum-quest-items.md §2.1-2.2.
 */
public record DeclensionFormPayload(
        String lemmaIast,
        String lemmaDevanagari,
        String morphologyClassCode,
        String gender,
        String caseType,
        String numberType,
        String correctFormIast,
        String correctFormDevanagari
) implements QuestItemPayload {
}
