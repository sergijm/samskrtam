package sm.selflearn.samskrtam.quest;

/**
 * Declension quest types served by curriculum-service (API v2).
 * See curriculum-quest-items.md §2 and quest-item-model.md §3.
 */
public final class GrammarQuestItemTypes {

    private GrammarQuestItemTypes() {
    }

    /** Type 2: lemma -> enter the word form (free text). */
    public static final QuestItemType DECLENSION_FORM = QuestItemTypes.of(
            "DECLENSION_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);

    /** Type 1: lemma -> pick the word form out of options. */
    public static final QuestItemType DECLENSION_FORM_CHOICE = QuestItemTypes.of(
            "DECLENSION_FORM_CHOICE", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** Type 3: word form -> identify case[, number[, gender]]. */
    public static final QuestItemType CASE_RECOGNITION = QuestItemTypes.of(
            "CASE_RECOGNITION", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** Type 4: word forms <-> case+number matching. */
    public static final QuestItemType DECLENSION_MATCH = QuestItemTypes.of(
            "DECLENSION_MATCH", QuestDomain.MORPHOLOGY, AnswerMode.MATCHING);
}
