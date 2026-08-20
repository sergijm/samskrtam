package sm.selflearn.samskrtam.quest.conjugation;

import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.QuestItemPayload;

import java.util.List;

/**
 * Payload for CONJUGATION_FORM and CONJUGATION_FORM_CHOICE (ver-form):
 * given a root + person + number + voice + tense, select/enter the verb form.
 */
public record ConjugationFormPayload(
        String lemmaIast,
        String lemmaDevanagari,
        String meaningRu,
        String voice,
        int person,
        String numberType,
        String tense,
        String correctFormIast,
        String correctFormDevanagari,
        List<HighlightToken> highlights
) implements QuestItemPayload {
}