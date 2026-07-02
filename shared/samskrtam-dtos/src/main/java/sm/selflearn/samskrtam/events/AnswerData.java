package sm.selflearn.samskrtam.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerData {
    private UUID questionId;
    private String questionText; // NEW: Add question text for statistics service
    private UUID selectedOptionId;
    private String correctFormIast;
    private Boolean isCorrect; // Changed from 'correct' to 'isCorrect'
    private Integer responseTimeMs; // Changed from int to Integer
    private Instant answeredAt;
    private String explanationRu; // New field
    private String explanationEn; // New field
}
