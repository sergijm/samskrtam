package sm.selflearn.samskrtam.quiz.event;

import sm.selflearn.samskrtam.content.dto.LessonType;

import java.time.Instant;
import java.util.UUID;

public record QuizSessionStatusChangedEvent(
    UUID quizSessionId,
    UUID userId,
    UUID lessonId,
    LessonType lessonType,
    String oldStatus,
    String newStatus,
    Instant timestamp
) implements StatisticEvent {}

