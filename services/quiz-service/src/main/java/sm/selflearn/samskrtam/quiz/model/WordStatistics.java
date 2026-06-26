package sm.selflearn.samskrtam.quiz.model;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.Instant;
import java.util.UUID;
@Data
@Builder
@Table(name = "word_statistics", schema = "quiz")
public class WordStatistics {
@Id
private UUID id;
private UUID userId;
private UUID vocabularyWordId;
private int totalAttempts;
private int correctAnswers;
private Instant lastSeenAt;
}