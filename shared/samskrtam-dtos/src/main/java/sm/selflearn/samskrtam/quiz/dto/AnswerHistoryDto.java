package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class AnswerHistoryDto {
    UUID questionId;
    int questionNumber;
    String questionText;
    String selectedAnswerIast;
    String correctOptionIast;
    Boolean isCorrect;
    Integer responseTimeMs;
    Instant answeredAt;
    String explanationRu;
    String explanationEn;
}
