package sm.selflearn.samskrtam.quest;

/**
 * Factory creating a simple record implementation of {@link QuestItemType}.
 * Adding a new type is one line in the corresponding holder class.
 */
public final class QuestItemTypes {

    private QuestItemTypes() {
    }

    public static QuestItemType of(String code, QuestDomain domain, AnswerMode defaultAnswerMode) {
        return new SimpleQuestItemType(code, domain, defaultAnswerMode);
    }

    private record SimpleQuestItemType(
            String code,
            QuestDomain domain,
            AnswerMode defaultAnswerMode
    ) implements QuestItemType {
    }
}
