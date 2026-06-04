package sm.selflearn.samskrtam.events;

import sm.selflearn.samskrtam.content.dto.QuizType;

import java.time.Instant;
import java.util.UUID;

public record SessionCompleted(
    UUID    eventId,
    Instant occurredAt,
    UUID    userId,
    QuizType quizType,
    UUID    quizId,
    int     score,
    int     totalQuestions,
    long    durationMs
) {
    public SessionCompleted(UUID userId, QuizType quizType, UUID quizId,
                            int score, int totalQuestions, long durationMs) {
        this(UUID.randomUUID(), Instant.now(), userId, quizType,
             quizId, score, totalQuestions, durationMs);
    }
}
