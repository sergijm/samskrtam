package sm.selflearn.samskrtam.quiz.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import sm.selflearn.samskrtam.content.dto.QuizType; // Corrected import

import java.time.Instant;
import java.util.UUID;

@Data
@Builder // Moved @Builder back to class level
@Table(name = "quiz_sessions", schema = "quiz")
public class QuizSession {
    @Id
    private UUID id;
    private UUID userId;
    private UUID quizId;
    private QuizType quizType;
    private int totalQuestions;
    private int answeredQuestions;
    private int score;
    private SessionStatus status;
    private Instant startedAt;
    private Instant completedAt;
}
