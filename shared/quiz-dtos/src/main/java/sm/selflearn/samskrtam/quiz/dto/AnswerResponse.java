package sm.selflearn.samskrtam.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class AnswerResponse {
    @JsonProperty("isCorrect") // Explicitly name the JSON property
    boolean isCorrect;
    UUID correctOptionId;
    String correctAnswerText;
    String explanationRu;
    String explanationEn;
    int questionNumber;
    int totalQuestions;
}
