package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.LessonType; // Corrected import

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class SessionHistoryResponse {
    UUID sessionId;
    UUID quizId;
    LessonType lessonType;
    int score;
    int totalQuestions;
    int percentage;
    long durationMs;
    Instant completedAt;
}
