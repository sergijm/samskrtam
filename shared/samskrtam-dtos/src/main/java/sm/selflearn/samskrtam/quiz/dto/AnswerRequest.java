package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized; // Import Jacksonized

import java.util.UUID;

@Value
@Builder
@Jacksonized // Add Jacksonized annotation
public class AnswerRequest {
    UUID questionId;
    UUID selectedOptionId;
    String selectedFormIast; // New optional field
    int responseTimeMs;
}
