package sm.selflearn.samskrtam.statistics.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class UserQuizStatisticDto {
    UUID quizId;
    LessonType lessonType;
    int totalSessions;
    int totalQuestionsAnswered;
    int totalCorrectAnswers;
    int totalScore;
    double averageScore;
    Instant lastCompletedAt;
    String answerHistoryJson; // New field for storing answer history as JSON
}
