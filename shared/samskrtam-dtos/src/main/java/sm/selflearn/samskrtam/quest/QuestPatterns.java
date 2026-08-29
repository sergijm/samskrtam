package sm.selflearn.samskrtam.quest;

/**
 * Quest pattern dictionary — cognitive-operation labels from quest_catalog_2.md.
 * <p>
 * A {@code quest_pattern} is a decorative label (domain + operation in one readable
 * code) attached to a {@code quest_item}; it must not influence answer checking
 * (driven by {@link AnswerMode}), session selection or progress (itemType +
 * progressTag). It serves i18n titles and analytics.
 * <p>
 * Letter codes follow quest_catalog_2.md: lowercase, hyphen-separated, each word
 * 3–5 chars, full code ≤ 9 chars.
 */
public final class QuestPatterns {

    private QuestPatterns() {
    }

    /** Verbs — form synthesis (given person/number/voice/tense → form). */
    public static final String VERB_FORM = "ver-form";
    /** Verbs — analysis (form → person/number/voice/tense). */
    public static final String VERB_ANAL = "ver-anal";
    /** Verbs — matching (forms ↔ their grammatical attributes). */
    public static final String VERB_MATCH = "ver-match";
    /** Verbs — classification (order forms into groups). */
    public static final String VERB_CLASS = "ver-class";
    /** Verbs — error correction. */
    public static final String VERB_FIX = "ver-fix";
    /** Verbs — fill-in sentence (select the correct verb form). */
    public static final String VERB_FILL = "ver-fill";
    /** Verbs — translate (RU → SA). */
    public static final String VERB_TRAN = "ver-tran";
    /** Verbs — translate (SA → RU). */
    public static final String VERB_REV = "ver-rev";
    /** Verbs — odd one out. */
    public static final String VERB_ODD = "ver-odd";
    /** Verbs — builder (root + suffix + ending → form). */
    public static final String VERB_BUILD = "ver-build";

    /** Nouns — form synthesis (stem/gender/case/number → form). */
    public static final String NOM_FORM = "nom-form";
    /** Nouns — analysis (form → case/number/gender). */
    public static final String NOM_ANAL = "nom-anal";
    /** Nouns — matching (forms ↔ case/number labels). */
    public static final String NOM_MATCH = "nom-match";
    /** Nouns — classification (order forms by case). */
    public static final String NOM_CLASS = "nom-class";
    /** Nouns — error correction. */
    public static final String NOM_FIX = "nom-fix";
    /** Nouns — fill-in sentence (select the correct form). */
    public static final String NOM_FILL = "nom-fill";
    /** Nouns — translate (RU → SA). */
    public static final String NOM_TRAN = "nom-tran";
    /** Nouns — translate (SA → RU). */
    public static final String NOM_REV = "nom-rev";
    /** Nouns — odd one out. */
    public static final String NOM_ODD = "nom-odd";
    /** Nouns — builder (stem + ending → form). */
    public static final String NOM_BUILD = "nom-build";

    /** Lexicon — direct translation (SA → RU meaning). */
    public static final String LEX_TRAN = "lex-tran";
    /** Lexicon — reverse translation (RU meaning → SA word). */
    public static final String LEX_REV = "lex-rev";
    /** Lexicon — synonyms. */
    public static final String LEX_SAME = "lex-same";
    /** Lexicon — antonyms. */
    public static final String LEX_ANT = "lex-ant";
    /** Lexicon — categorization into a semantic class. */
    public static final String LEX_CAT = "lex-cat";
    /** Lexicon — contextual fill (select the word fitting the sentence). */
    public static final String LEX_FILL = "lex-fill";
    /** Lexicon — etymology (word → root). */
    public static final String LEX_ROOT = "lex-root";
    /** Lexicon — multiple meanings (multi-select). */
    public static final String LEX_POLY = "lex-poly";
    /** Lexicon — matching (words ↔ translations). */
    public static final String LEX_MATCH = "lex-match";
    /** Lexicon — classification (order words by thematic groups). */
    public static final String LEX_CLASS = "lex-class";
    /** Lexicon — odd one out. */
    public static final String LEX_ODD = "lex-odd";
    /** Lexicon — anagrams. */
    public static final String LEX_ANAG = "lex-anag";
    /** Lexicon — sentence puzzle (words in disorder → sentence). */
    public static final String LEX_PUZ = "lex-puz";
    /** Lexicon — word by description. */
    public static final String LEX_NAME = "lex-name";
    /** Lexicon — numerals. */
    public static final String LEX_NUM = "lex-num";

    /** Sandhi — join (apply sandhi to two words). */
    public static final String SND_JOIN = "san-join";
    /** Sandhi — split (restore source words). */
    public static final String SND_SPLIT = "san-split";
    /** Sandhi — matching (rule ↔ example). */
    public static final String SND_MATCH = "san-match";
    /** Sandhi — classify (determine sandhi type). */
    public static final String SND_CLASS = "san-class";
    /** Sandhi — error correction. */
    public static final String SND_FIX = "san-fix";
    /** Sandhi — choose the correct variant. */
    public static final String SND_PICK = "san-pick";
    /** Sandhi — undo sentence into source words. */
    public static final String SND_UNDO = "san-undo";
    /** Sandhi — transliteration (Devanagari → IAST). */
    public static final String SND_TRAN = "san-tran";
    /** Sandhi — chain (apply sandhi sequentially). */
    public static final String SND_CHAIN = "san-chain";
    /** Sandhi — scan text for sandhi instances. */
    public static final String SND_SCAN = "san-scan";
    /** Sandhi — build (create sandhi yourself). */
    public static final String SND_BUILD = "san-build";
}