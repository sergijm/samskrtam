package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class QuestionResponse {
    UUID id;
    int questionNumber; // New field
    String text;
    String explanationRu; // Changed from explanation
    String explanationEn; // Added explanationEn
    UUID declensionStemId; // For quiz-service to generate distractors
    CaseType targetCase;       // For quiz-service to generate distractors
    NumberType targetNumber;   // For quiz-service to generate distractors
    String correctFormIast; // The correct form in IAST
    String correctFormDevanagari; // The correct form in Devanagari
    String stem; // New field
}
