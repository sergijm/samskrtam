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

    // ----------------------------------------------------------------------
    // Stub types (registered, no generator yet) — quest_catalog_2.md LEX-* patterns.
    // ----------------------------------------------------------------------

    /** LEX-REV: meaning -> pick the Sanskrit word. */
    public static final QuestItemType VOCABULARY_RECALL = QuestItemTypes.of(
            "VOCABULARY_RECALL", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);

    /** LEX-SAME: word -> pick its synonym. */
    public static final QuestItemType VOCABULARY_SYNONYM = QuestItemTypes.of(
            "VOCABULARY_SYNONYM", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);

    /** LEX-ANT: word -> pick its antonym. */
    public static final QuestItemType VOCABULARY_ANTONYM = QuestItemTypes.of(
            "VOCABULARY_ANTONYM", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);

    /** LEX-ROOT: word -> derive its root (dhAtu). */
    public static final QuestItemType VOCABULARY_ROOT = QuestItemTypes.of(
            "VOCABULARY_ROOT", QuestDomain.LEXICON, AnswerMode.FREE_TEXT);

    /** LEX-CAT: word -> its thematic category. */
    public static final QuestItemType VOCABULARY_SEMANTIC_FIELD = QuestItemTypes.of(
            "VOCABULARY_SEMANTIC_FIELD", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);

    /** Word form -> its grammatical gender. */
    public static final QuestItemType VOCABULARY_GENDER = QuestItemTypes.of(
            "VOCABULARY_GENDER", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);

    /** LEX-ODD: find the word that does not fit the group. */
    public static final QuestItemType VOCABULARY_ODD = QuestItemTypes.of(
            "VOCABULARY_ODD", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);

    /** LEX-ANAG: reassemble a word from an anagram. */
    public static final QuestItemType VOCABULARY_ANAGRAM = QuestItemTypes.of(
            "VOCABULARY_ANAGRAM", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);

    /** LEX-MATCH: Sanskrit words <-> translations. */
    public static final QuestItemType VOCABULARY_MATCH = QuestItemTypes.of(
            "VOCABULARY_MATCH", QuestDomain.LEXICON, AnswerMode.MATCHING);

    /** LEX-POLY: select all correct meanings of a word. */
    public static final QuestItemType VOCABULARY_POLYSEMY = QuestItemTypes.of(
            "VOCABULARY_POLYSEMY", QuestDomain.LEXICON, AnswerMode.MULTI_SELECT);

    /** LEX-FILL: word fitting the gap in a sentence (contextual choice). */
    public static final QuestItemType VOCABULARY_CONTEXTUAL = QuestItemTypes.of(
            "VOCABULARY_CONTEXTUAL", QuestDomain.LEXICON, AnswerMode.SINGLE_CHOICE);

    /** LEX-CLASS: order words by thematic groups. */
    public static final QuestItemType VOCABULARY_CLASSIFY = QuestItemTypes.of(
            "VOCABULARY_CLASSIFY", QuestDomain.LEXICON, AnswerMode.MATCHING);
}
