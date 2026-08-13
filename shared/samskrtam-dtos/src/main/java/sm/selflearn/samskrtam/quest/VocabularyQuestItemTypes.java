package sm.selflearn.samskrtam.quest;

/**
 * Lexical quest types materialized by curriculum-service (API v2).
 * See curriculum-quest-items.md and lexical-quizzes.md.
 */
public final class VocabularyQuestItemTypes {

    private VocabularyQuestItemTypes() {
    }

    /** Lemma -> pick the meaning out of options (recognition direction). */
    public static final QuestItemType VOCABULARY_WORD = QuestItemTypes.of(
            "VOCABULARY_WORD", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);
}
