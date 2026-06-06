package sm.selflearn.samskrtam.statistics.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.QuizType;

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
}
