package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * "Today" block — what the user should do right now (review / new / weak).
 * Per-user counters; currently returned as plausible random values until real
 * spaced-repetition state is wired up (see docs/docs/services/lexical-quizzes.md).
 */
public record LexiconTodayDto(
        int reviewDue,
        int newWords,
        int weakWords
) {
}