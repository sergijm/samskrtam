package sm.selflearn.samskrtam.quest;

/**
 * Sandhi quest types (PHONOLOGY domain). Registered as stubs — no generator yet.
 * Pattern codes from quest_catalog_2.md (SND-*): {@link QuestPatterns#SND_JOIN},
 * {@link QuestPatterns#SND_SPLIT}, {@link QuestPatterns#SND_MATCH},
 * {@link QuestPatterns#SND_CLASS}, {@link QuestPatterns#SND_FIX},
 * {@link QuestPatterns#SND_PICK}, {@link QuestPatterns#SND_UNDO},
 * {@link QuestPatterns#SND_TRAN}, {@link QuestPatterns#SND_CHAIN},
 * {@link QuestPatterns#SND_SCAN}, {@link QuestPatterns#SND_BUILD}.
 */
public final class PhonologyQuestItemTypes {

    private PhonologyQuestItemTypes() {
    }

    /** SND-ANL/RVS/SCAN: sandhi word/sentence -> restore source words. */
    public static final QuestItemType SANDHI_SPLIT = QuestItemTypes.of(
            "SANDHI_SPLIT", QuestDomain.PHONOLOGY, AnswerMode.FREE_TEXT);

    /** SND-JOIN/PICK: two words -> one by applying sandhi rules. */
    public static final QuestItemType SANDHI_JOIN = QuestItemTypes.of(
            "SANDHI_JOIN", QuestDomain.PHONOLOGY, AnswerMode.SINGLE_CHOICE);

    /** SND-MATCH: sandhi rule <-> correct example. */
    public static final QuestItemType SANDHI_MATCH = QuestItemTypes.of(
            "SANDHI_MATCH", QuestDomain.PHONOLOGY, AnswerMode.MATCHING);

    /** SND-CLASS: determine the sandhi type of a joined word. */
    public static final QuestItemType SANDHI_CLASSIFY = QuestItemTypes.of(
            "SANDHI_CLASSIFY", QuestDomain.PHONOLOGY, AnswerMode.SINGLE_CHOICE);

    /** SND-FIX: wrong sandhi -> pick the correct variant. */
    public static final QuestItemType SANDHI_CORRECTION = QuestItemTypes.of(
            "SANDHI_CORRECTION", QuestDomain.PHONOLOGY, AnswerMode.SINGLE_CHOICE);

    /** SND-CHAIN: join several words applying sandhi sequentially. */
    public static final QuestItemType SANDHI_CHAIN = QuestItemTypes.of(
            "SANDHI_CHAIN", QuestDomain.PHONOLOGY, AnswerMode.FREE_TEXT);

    /** SND-TRAN: Devanagari -> IAST transliteration. */
    public static final QuestItemType SANDHI_TRANSLITERATION = QuestItemTypes.of(
            "SANDHI_TRANSLITERATION", QuestDomain.PHONOLOGY, AnswerMode.FREE_TEXT);

    /** SND-BUILD: produce the sandhi result for a given pair of words. */
    public static final QuestItemType SANDHI_BUILD = QuestItemTypes.of(
            "SANDHI_BUILD", QuestDomain.PHONOLOGY, AnswerMode.FREE_TEXT);
}