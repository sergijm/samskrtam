package sm.selflearn.samskrtam.quiz.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class AnswerResponse {
    @JsonProperty("isCorrect")
    boolean correct;

    /** Single correct option ID (for single-select questions). */
    UUID correctOptionId;

    /** Multiple correct option IDs (for multi-select questions, e.g. CASE_BY_FORM). */
    List<UUID> correctOptionIds;

    String correctAnswerText;
    String explanationRu;
    String explanationEn;
    int questionNumber;
    int totalQuestions;
}

