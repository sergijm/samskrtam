package sm.selflearn.samskrtam.quiz.event;

import sm.selflearn.samskrtam.content.dto.QuizType;
import java.time.Instant;
import java.util.UUID;

public record QuizAnsweredEvent(
    UUID quizSessionId,
    UUID userId,
    UUID quizId,
    QuizType quizType,
    UUID questionId,
    String userAnswer,
    boolean isCorrect,
    Instant timestamp
) implements StatisticEvent {}
