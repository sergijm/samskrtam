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

    /**
     * Stable progress-tag set id (NEW/LEARNING/MASTERED/DIFFICULT/SINGULAR/DUAL/PLURAL/...).
     * Null — session over the whole lesson without a slice. Participates in IN_PROGRESS
     * resume lookup by equality. Replaces the old filter/status columns dropped in V18.
     */
    private String progressTagSetId;
}