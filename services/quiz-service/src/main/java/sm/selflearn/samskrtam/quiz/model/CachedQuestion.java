package sm.selflearn.samskrtam.quiz.model;

import com.fasterxml.jackson.annotation.JsonCreator; // Import JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty; // Import JsonProperty
import lombok.Builder;
import lombok.Data;
import sm.selflearn.samskrtam.content.model.Case;
import sm.selflearn.samskrtam.content.model.Number;

import java.util.UUID;

@Data
public class CachedQuestion {
    private UUID questionId;
    private String text;
    private String explanationRu; // Changed from explanation
    private String explanationEn; // Added explanationEn
    private UUID declensionStemId; // For declension quizzes
    private Case targetCase;       // For declension quizzes
    private Number targetNumber;   // For declension quizzes
    private String correctFormIast; // The correct form in IAST
    private String correctFormDevanagari; // The correct form in Devanagari

    @JsonCreator
    @Builder // Keep @Builder for convenience in creating instances
    public CachedQuestion(
            @JsonProperty("questionId") UUID questionId,
            @JsonProperty("text") String text,
            @JsonProperty("explanationRu") String explanationRu, // Changed from explanation
            @JsonProperty("explanationEn") String explanationEn, // Added explanationEn
            @JsonProperty("declensionStemId") UUID declensionStemId,
            @JsonProperty("targetCase") Case targetCase,
            @JsonProperty("targetNumber") Number targetNumber,
            @JsonProperty("correctFormIast") String correctFormIast,
            @JsonProperty("correctFormDevanagari") String correctFormDevanagari) {
        this.questionId = questionId;
        this.text = text;
        this.explanationRu = explanationRu;
        this.explanationEn = explanationEn;
        this.declensionStemId = declensionStemId;
        this.targetCase = targetCase;
        this.targetNumber = targetNumber;
        this.correctFormIast = correctFormIast;
        this.correctFormDevanagari = correctFormDevanagari;
    }
}
