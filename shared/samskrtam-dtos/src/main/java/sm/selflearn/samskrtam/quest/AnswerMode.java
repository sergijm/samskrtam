package sm.selflearn.samskrtam.quest;

/**
 * How an answer is accepted and checked. One value can serve many quest types.
 */
public enum AnswerMode {
    /** Free-text input of a word form / translation. */
    FREE_TEXT,
    /** Selection of one option from distractors. */
    SINGLE_CHOICE,
    /** Selection of several options (e.g. all members of a compound). */
    MULTI_SELECT,
    /** Selection of a text span (e.g. sandhi boundaries in a line). */
    SPAN_SELECT,
    /** Matching pairs from two lists (e.g. word form <-> case/number). */
    MATCHING
}
