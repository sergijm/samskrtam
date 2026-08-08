package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Состояние user_lexeme_progress для одной лексемы (task-curriculum-15 §8/§9).
 */
public record LexemeProgressDto(
        UUID lexemeId,
        short masteryScore,
        int exposureCount,
        int correctCount,
        int incorrectCount,
        Instant lastSeenAt,
        Instant nextReviewAt
) {
}