package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import sm.selflearn.samskrtam.content.model.Case;
import sm.selflearn.samskrtam.content.model.Number;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class QuestionResponse {
    UUID id;
    String text;
    String explanation;
    UUID declensionStemId; // For quiz-service to generate distractors
    Case targetCase;       // For quiz-service to generate distractors
    Number targetNumber;   // For quiz-service to generate distractors
    QuestionOptionResponse correctOption; // Only the correct option is sent
}
