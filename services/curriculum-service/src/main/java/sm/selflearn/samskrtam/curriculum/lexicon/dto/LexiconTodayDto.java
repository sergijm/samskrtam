package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * "Today" block — what the user should do right now (review / new / weak).
 * Derived from {@code user_lexeme_progress}: review is due spaced-repetition,
 * new is unseen words, weak is studied but not yet mastered.
 */
public record LexiconTodayDto(
        int reviewDue,
        int newWords,
        int weakWords
) {
}