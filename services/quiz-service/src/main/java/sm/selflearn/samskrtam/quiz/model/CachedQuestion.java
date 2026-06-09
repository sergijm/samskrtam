package sm.selflearn.samskrtam.quiz.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import sm.selflearn.samskrtam.content.model.Case;
import sm.selflearn.samskrtam.content.model.Number;

import java.util.UUID;

@Data
public class CachedQuestion {
    private UUID questionId;
    private int questionNumber; // New field
    private String text;
    private String explanationRu;
    private String explanationEn;

    // For declension quizzes
    private UUID declensionStemId;
    private Case targetCase;
    private Number targetNumber;

    // For vocabulary quizzes
    private UUID vocabularyWordId;
    private QuestionLanguage questionSourceLanguage; // e.g., SANSKRIT, ENGLISH, RUSSIAN
    private QuestionLanguage questionTargetLanguage; // e.g., SANSKRIT, ENGLISH, RUSSIAN
    private String correctTranslationRu; // Correct translation in Russian
    private String correctTranslationEn; // Correct translation in English

    private String correctFormIast; // The correct form in IAST (can be declension or vocabulary)
    private String correctFormDevanagari; // The correct form in Devanagari (can be declension or vocabulary)

    @JsonCreator
    @Builder
    public CachedQuestion(
            @JsonProperty("questionId") UUID questionId,
            @JsonProperty("questionNumber") int questionNumber, // Add to constructor
            @JsonProperty("text") String text,
            @JsonProperty("explanationRu") String explanationRu,
            @JsonProperty("explanationEn") String explanationEn,
            @JsonProperty("declensionStemId") UUID declensionStemId,
            @JsonProperty("targetCase") Case targetCase,
            @JsonProperty("targetNumber") Number targetNumber,
            @JsonProperty("vocabularyWordId") UUID vocabularyWordId,
            @JsonProperty("questionSourceLanguage") QuestionLanguage questionSourceLanguage,
            @JsonProperty("questionTargetLanguage") QuestionLanguage questionTargetLanguage,
            @JsonProperty("correctTranslationRu") String correctTranslationRu,
            @JsonProperty("correctTranslationEn") String correctTranslationEn,
            @JsonProperty("correctFormIast") String correctFormIast,
            @JsonProperty("correctFormDevanagari") String correctFormDevanagari) {
        this.questionId = questionId;
        this.questionNumber = questionNumber; // Assign
        this.text = text;
        this.explanationRu = explanationRu;
        this.explanationEn = explanationEn;
        this.declensionStemId = declensionStemId;
        this.targetCase = targetCase;
        this.targetNumber = targetNumber;
        this.vocabularyWordId = vocabularyWordId;
        this.questionSourceLanguage = questionSourceLanguage;
        this.questionTargetLanguage = questionTargetLanguage;
        this.correctTranslationRu = correctTranslationRu;
        this.correctTranslationEn = correctTranslationEn;
        this.correctFormIast = correctFormIast;
        this.correctFormDevanagari = correctFormDevanagari;
    }
}
