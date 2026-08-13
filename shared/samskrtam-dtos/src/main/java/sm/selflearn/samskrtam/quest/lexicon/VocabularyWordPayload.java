package sm.selflearn.samskrtam.quest.lexicon;

import sm.selflearn.samskrtam.quest.QuestItemPayload;

/**
 * Payload of a VOCABULARY_WORD item in the recognition direction
 * (lemma -> meaning). See lexical-quizzes.md §1.
 */
public record VocabularyWordPayload(
        String lemmaSlp1,
        String lemmaIast,
        String lemmaDevanagari,
        String glossEn,
        String glossRu
) implements QuestItemPayload {
}
