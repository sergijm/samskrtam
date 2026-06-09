package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AnswerResponse {
    Boolean isCorrect;
    UUID correctOptionId;
    String explanationRu;
    String explanationEn;
    int questionNumber;
    int totalQuestions;
}
