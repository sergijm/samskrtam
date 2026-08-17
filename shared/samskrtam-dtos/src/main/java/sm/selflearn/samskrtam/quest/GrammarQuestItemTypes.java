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

    // ----------------------------------------------------------------------
    // Stub types (registered, no generator yet) — quest_catalog_2.md patterns
    // NOM-CLS / NOM-ODD / NOM-ERR and the CONJUGATION family (VBL-*).
    // ----------------------------------------------------------------------

    /** NOM-CLS: order word forms by case/number groups. */
    public static final QuestItemType DECLENSION_CLASSIFY = QuestItemTypes.of(
            "DECLENSION_CLASSIFY", QuestDomain.MORPHOLOGY, AnswerMode.MATCHING);

    /** NOM-ODD: find the word form that does not fit the group. */
    public static final QuestItemType DECLENSION_ODD = QuestItemTypes.of(
            "DECLENSION_ODD", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** NOM-ERR: a form is wrong — pick the correct one. */
    public static final QuestItemType DECLENSION_CORRECTION = QuestItemTypes.of(
            "DECLENSION_CORRECTION", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** VBL-SYN (free text): root+person+number+voice+tense -> verb form. */
    public static final QuestItemType CONJUGATION_FORM = QuestItemTypes.of(
            "CONJUGATION_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);

    /** VBL-SYN (choice): root+person+number+voice+tense -> pick the verb form. */
    public static final QuestItemType CONJUGATION_FORM_CHOICE = QuestItemTypes.of(
            "CONJUGATION_FORM_CHOICE", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** VBL-ANL: verb form -> person/number/voice/tense. */
    public static final QuestItemType CONJUGATION_ANALYSIS = QuestItemTypes.of(
            "CONJUGATION_ANALYSIS", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** VBL-MCH: verb forms <-> grammatical attributes. */
    public static final QuestItemType CONJUGATION_MATCH = QuestItemTypes.of(
            "CONJUGATION_MATCH", QuestDomain.MORPHOLOGY, AnswerMode.MATCHING);

    /** VBL-CLS: order verb forms into groups. */
    public static final QuestItemType CONJUGATION_CLASSIFY = QuestItemTypes.of(
            "CONJUGATION_CLASSIFY", QuestDomain.MORPHOLOGY, AnswerMode.MATCHING);

    /** VBL-ERR: wrong verb form -> pick the correct one. */
    public static final QuestItemType CONJUGATION_CORRECTION = QuestItemTypes.of(
            "CONJUGATION_CORRECTION", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** VBL-FIT: sentence with a gap -> pick the verb form. */
    public static final QuestItemType CONJUGATION_FIT = QuestItemTypes.of(
            "CONJUGATION_FIT", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** VBL-TRN: translate (RU -> SA) a verb phrase. */
    public static final QuestItemType CONJUGATION_TRANSLATE = QuestItemTypes.of(
            "CONJUGATION_TRANSLATE", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** VBL-RVS: verb form -> Russian meaning. */
    public static final QuestItemType CONJUGATION_RECALL = QuestItemTypes.of(
            "CONJUGATION_RECALL", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** VBL-ODD: find the verb form that does not fit the group. */
    public static final QuestItemType CONJUGATION_ODD = QuestItemTypes.of(
            "CONJUGATION_ODD", QuestDomain.MORPHOLOGY, AnswerMode.SINGLE_CHOICE);

    /** VBL-BLD: assemble a verb form from root+suffix+ending. */
    public static final QuestItemType CONJUGATION_BUILD = QuestItemTypes.of(
            "CONJUGATION_BUILD", QuestDomain.MORPHOLOGY, AnswerMode.MATCHING);

    /** LEX-NUM: numerals and their declension. */
    public static final QuestItemType NUMERAL_FORM = QuestItemTypes.of(
            "NUMERAL_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);

    /** Present/future/past participles (declined like adjectives). */
    public static final QuestItemType PARTICIPLE_FORM = QuestItemTypes.of(
            "PARTICIPLE_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);

    /** Absolutive (ktvA/lyap) — indeclinable, single form per verb. */
    public static final QuestItemType ABSOLUTIVE_FORM = QuestItemTypes.of(
            "ABSOLUTIVE_FORM", QuestDomain.MORPHOLOGY, AnswerMode.FREE_TEXT);
}
