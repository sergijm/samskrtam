package sm.selflearn.samskrtam.statistics.model;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sm.selflearn.samskrtam.content.dto.LessonType;
import java.time.Instant;
import java.util.UUID;
@Entity
@Table(name = "user_quiz_session_statistics", schema = "statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserQuizSessionStatistic {
@Id
    private UUID id;
@Column(nullable = false)
    private UUID userId;
@Column(name = "quiz_id", nullable = false)
    private UUID lessonId;
@Enumerated(EnumType.STRING)
@Column(name = "lesson_type", nullable = false)
    private LessonType lessonType;
@Column(nullable = false)
    private int totalSessions;
@Column(nullable = false)
    private int totalQuestionsAnswered;
@Column(nullable = false)
    private int totalCorrectAnswers;
@Column(nullable = false)
    private int totalScore;
@Column(nullable = false)
    private double averageScore;
// nullable — заполняется только после первого завершения сессии
@Column(nullable = true)
    private Instant lastCompletedAt;
}

