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
public class GeneratedQuizQuestionDto {
    UUID id;
    UUID generatedQuizDataId;
    UUID quizId;
    int questionNumber;
    String text;
    String explanationRu;
    String explanationEn;
    UUID declensionStemId;
    String stemDevanagari;       // NEW: devanagari of the stem
    String stemTranslationRu;    // NEW: russian translation of the stem
    String stemTranslationEn;    // NEW: english translation of the stem
    CaseType targetCase;
    NumberType targetNumber;
    String correctFormIast;
    String correctFormDevanagari;
    UUID vocabularyWordId;
    QuestionLanguage questionSourceLanguage;
    QuestionLanguage questionTargetLanguage;
    String correctTranslationRu;
    String correctTranslationEn;
    String userLocale;
    String stem;
    String caseType;
    String numberType;

    /** Gender from the declension stem, used by quiz-service for progress aggregation */
    String gender;
}

