package sm.selflearn.samskrtam.quest.conjugation;

import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.QuestItemPayload;

import java.util.List;

/**
 * Payload for CONJUGATION_BUILD (ver-build):
 * assemble the verb form from root + suffix + ending components.
 */
public record ConjugationBuildPayload(
        String lemmaIast,
        String lemmaDevanagari,
        String meaningRu,
        String voice,
        int person,
        String numberType,
        String tense,
        List<String> components,
        String correctFormIast,
        String correctFormDevanagari,
        List<HighlightToken> highlights
) implements QuestItemPayload {
}