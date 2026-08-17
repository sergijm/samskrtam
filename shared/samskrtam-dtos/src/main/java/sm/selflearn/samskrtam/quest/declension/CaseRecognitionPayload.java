package sm.selflearn.samskrtam.quest.declension;

import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.QuestItemPayload;

import java.util.List;

/**
 * Payload for CASE_RECOGNITION: given a word form, identify case[, number[, gender]].
 * See curriculum-quest-items.md §2.3.
 */
public record CaseRecognitionPayload(
        String wordFormIast,
        String wordFormDevanagari,
        String lemmaIast,
        String morphologyClassCode,
        String correctCaseType,
        String correctNumberType,
        String correctGender,
        boolean genderRequired,
        List<String> distractorCombinations,
        List<HighlightToken> highlights
) implements QuestItemPayload {
}