package sm.selflearn.samskrtam.quiz.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Table(name = "word_score", schema = "quiz")
public class WordScore {
    @Id
    private UUID id;
    private UUID userId;
    private UUID wordId;
    private UUID lessonId;
    private int score;
    private Instant updatedAt;
}