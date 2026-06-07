package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class QuizSessionSummaryDto {
    UUID sessionId;
    UUID quizId;
    String quizTitle; // Название квиза
    QuizType quizType;
    int score;
    int totalQuestions;
    SessionStatus status;
    Instant startedAt;
    Instant completedAt; // Будет null, если сессия не завершена
    Long durationMs; // Длительность сессии в миллисекундах
}
