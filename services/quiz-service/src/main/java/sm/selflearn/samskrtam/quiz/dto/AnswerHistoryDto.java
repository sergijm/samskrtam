package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class AnswerHistoryDto {
    UUID questionId;
    String questionText;
    String selectedAnswerIast; // Changed from selectedOptionId and selectedOptionText
    String correctOptionIast; // Changed from correctOptionId and correctOptionText
    Boolean isCorrect;
    Integer responseTimeMs;
    Instant answeredAt;
    String explanation; // Localized explanation
}
