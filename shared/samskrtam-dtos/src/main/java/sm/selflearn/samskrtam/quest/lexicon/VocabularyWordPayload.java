package sm.selflearn.samskrtam.quest.lexicon;

import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.QuestItemPayload;

import java.util.List;

/**
 * Payload of a VOCABULARY_WORD item in the recognition direction
 * (lemma -> meaning). See lexical-quizzes.md §1.
 */
public record VocabularyWordPayload(
        String lemmaSlp1,
        String lemmaIast,
        String lemmaDevanagari,
        String glossEn,
        String glossRu,
        List<HighlightToken> highlights,
        int order
) implements QuestItemPayload {
}