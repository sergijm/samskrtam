package sm.selflearn.samskrtam.events;


import sm.selflearn.samskrtam.content.dto.QuizType;

import java.time.Instant;
import java.util.UUID;

public record AnswerSubmitted(
    UUID    eventId,
    Instant occurredAt,
    UUID    userId,
    QuizType quizType,
    UUID    quizId,
    UUID    questionId,
    UUID    selectedOptionId,
    boolean isCorrect,
    int     responseTimeMs
) {
    public AnswerSubmitted(UUID userId, QuizType quizType, UUID quizId,
                           UUID questionId, UUID selectedOptionId,
                           boolean isCorrect, int responseTimeMs) {
        this(UUID.randomUUID(), Instant.now(), userId, quizType,
             quizId, questionId, selectedOptionId, isCorrect, responseTimeMs);
    }
}
