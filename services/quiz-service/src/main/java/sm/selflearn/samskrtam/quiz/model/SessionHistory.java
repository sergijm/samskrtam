package sm.selflearn.samskrtam.quiz.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder // Moved @Builder back to class level
@Table(name = "session_history", schema = "quiz")
public class SessionHistory {
    @Id
    private UUID id;
    private UUID sessionId;
    private UUID userId;
    private UUID lessonId;
    private LessonType lessonType;
    private int score;
    private int totalQuestions;
    private long durationMs;
    private Instant completedAt;
}
