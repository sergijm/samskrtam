package sm.selflearn.samskrtam.statistics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuizSessionStatistic {
    private UUID id;

    private UUID userId;

    private UUID quizId;

    private LessonType lessonType;

    private int totalSessions;

    private int totalQuestionsAnswered;

    private int totalCorrectAnswers;

    private int totalScore;

    private double averageScore;

    private Instant lastCompletedAt;
}
