package sm.selflearn.samskrtam.statistics.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.QuizType; // Corrected import to quiz-content-dtos

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class UserQuizStatisticDto {
    UUID quizId;
    QuizType quizType;
    int totalSessions;
    int totalQuestionsAnswered;
    int totalCorrectAnswers;
    int totalScore;
    double averageScore;
    Instant lastCompletedAt;
    String answerHistoryJson; // New field for storing answer history as JSON
}
