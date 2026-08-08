package sm.selflearn.samskrtam.quest;

/**
 * Identifier of a quest type. Implementations are constants in per-domain
 * holders (e.g. GrammarQuestItemTypes). Open registry: a new type is
 * registered without modifying existing code.
 */
public interface QuestItemType {

    /** "DECLENSION_FORM", "CONJUGATION_FORM", "VOCABULARY_SYNONYM" ... */
    String code();

    QuestDomain domain();

    AnswerMode defaultAnswerMode();
}
