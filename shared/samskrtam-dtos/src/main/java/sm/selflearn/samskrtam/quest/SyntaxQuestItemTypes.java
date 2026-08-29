package sm.selflearn.samskrtam.quest;

/**
 * Quest types for the SYNTAX domain — case meanings and karaka roles.
 */
public final class SyntaxQuestItemTypes {

    private SyntaxQuestItemTypes() {
    }

    /** Case meaning recognition: given a case or example, identify its semantic role. */
    public static final QuestItemType CASE_MEANING = QuestItemTypes.of(
            "CASE_MEANING", QuestDomain.SYNTAX, AnswerMode.SINGLE_CHOICE);
}