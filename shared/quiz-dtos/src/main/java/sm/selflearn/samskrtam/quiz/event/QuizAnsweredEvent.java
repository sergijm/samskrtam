package sm.selflearn.samskrtam.quiz.event;

import sm.selflearn.samskrtam.content.dto.LessonType;

import java.time.Instant;
import java.util.UUID;

public record QuizAnsweredEvent(
    UUID quizSessionId,
    UUID userId,
    UUID quizId,
    LessonType lessonType,
    UUID questionId,
    String userAnswer,
    boolean isCorrect,
    Instant timestamp
) implements StatisticEvent {}
