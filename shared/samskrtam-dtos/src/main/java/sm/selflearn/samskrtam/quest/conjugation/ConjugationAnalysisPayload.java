package sm.selflearn.samskrtam.quest.conjugation;

import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.QuestItemPayload;

import java.util.List;

/**
 * Payload for CONJUGATION_ANALYSIS (ver-anal):
 * given a verb form, identify its person, number, voice and tense.
 */
public record ConjugationAnalysisPayload(
        String wordFormIast,
        String wordFormDevanagari,
        String lemmaIast,
        String meaningRu,
        int correctPerson,
        String correctNumberType,
        String correctVoice,
        String correctTense,
        List<String> distractorCombinations,
        List<HighlightToken> highlights
) implements QuestItemPayload {
}