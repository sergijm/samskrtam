package sm.selflearn.samskrtam.quest.conjugation;

import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.QuestItemPayload;

import java.util.List;

/**
 * Payload for CONJUGATION_MATCH (ver-match):
 * match verb forms of one lemma to their person/number/voice attributes.
 */
public record ConjugationMatchPayload(
        String lemmaIast,
        String meaningRu,
        String tense,
        List<ConjugationMatchPair> pairs,
        List<HighlightToken> highlights
) implements QuestItemPayload {

    public record ConjugationMatchPair(
            String pairId,
            String wordFormIast,
            String wordFormDevanagari,
            int person,
            String numberType,
            String voice
    ) {
    }
}