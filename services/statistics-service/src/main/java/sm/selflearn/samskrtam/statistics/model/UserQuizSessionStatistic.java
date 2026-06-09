package sm.selflearn.samskrtam.statistics.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sm.selflearn.samskrtam.content.dto.QuizType; // Corrected import to quiz-content-dtos

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID quizId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuizType quizType;

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

    @Column(nullable = false)
    private Instant lastCompletedAt;

    @Column(columnDefinition = "TEXT") // Store JSON as TEXT
    private String answerHistoryJson; // New field for storing answer history as JSON
}
