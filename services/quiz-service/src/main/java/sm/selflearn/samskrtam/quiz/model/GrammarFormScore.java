package sm.selflearn.samskrtam.quiz.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "grammar_form_score", schema = "quiz")
public class GrammarFormScore {

    @Id
    private UUID id;

    private UUID userId;

    private UUID lessonId;

    private String caseType;     // "NOMINATIVE", "ACCUSATIVE", ...

    private String numberType;   // "SINGULAR", "DUAL", "PLURAL"

    private int score;

    private Instant updatedAt;
}
