package sm.selflearn.samskrtam.quiz.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Table(name = "quiz_session", schema = "quiz")
public class QuizSession {
    @Id
    private UUID id;
    private UUID userId;
    private UUID lessonId;
    private LessonType lessonType;
    private int totalQuestions;
    private int answeredQuestions;
    private int score;
    private SessionStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private String vocabularyWordsJson;

    // Filter columns for filtered quiz sessions (see docs/quizzes/quiz-declension.md §3.4)
    private FilterScope filterScope;
    private String filterCaseType;
    private String filterNumberType;
    private String filterGender;
}

