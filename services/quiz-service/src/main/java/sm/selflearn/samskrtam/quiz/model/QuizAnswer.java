package sm.selflearn.samskrtam.quiz.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder // Moved @Builder back to class level
@Table(name = "quiz_answers", schema = "quiz")
public class QuizAnswer {
    @Id
    private UUID id;
    private UUID sessionId;
    private UUID questionId;
    private UUID selectedOptionId;
    private String selectedFormIast; // New field to store the IAST of the selected option
    private String correctFormIast; // Store the correct form IAST
    private boolean correct;
    private int responseTimeMs;
    private Instant answeredAt;
}
