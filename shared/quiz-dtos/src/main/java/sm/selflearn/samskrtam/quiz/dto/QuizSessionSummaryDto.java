package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.LessonType; // Corrected import
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class QuizSessionSummaryDto {
    UUID sessionId;
    UUID quizId;
    String quizTitle;
    String quizTitleRu;
    String quizTitleEn;
    String slug;
    LessonType lessonType;
    int score;
    int totalQuestions;
    SessionStatus status;
    Instant startedAt;
    Instant completedAt;
    Long durationMs;
}
