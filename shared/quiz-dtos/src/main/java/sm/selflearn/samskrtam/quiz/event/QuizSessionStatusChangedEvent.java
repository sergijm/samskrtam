package sm.selflearn.samskrtam.quiz.event;

import sm.selflearn.samskrtam.content.dto.QuizType;
import java.time.Instant;
import java.util.UUID;

public record QuizSessionStatusChangedEvent(
    UUID quizSessionId,
    UUID userId,
    UUID quizId,
    QuizType quizType,
    String oldStatus,
    String newStatus,
    Instant timestamp
) implements StatisticEvent {}
