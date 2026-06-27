package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import sm.selflearn.samskrtam.content.model.Case; // Corrected import
import sm.selflearn.samskrtam.content.model.Number; // Corrected import

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GeneratedQuizQuestionDto {
    UUID id; // Unique ID for this generated question
    UUID generatedQuizDataId; // New field to link to GeneratedQuizDataRecord
    UUID quizId; // The quiz this question belongs to
    int questionNumber; // New field for the order of the question
    String text; // This will be a more general question text, not containing stem/case/number
    String explanationRu;
    String explanationEn;
    UUID declensionStemId;
    Case targetCase;
    Number targetNumber;
    String correctFormIast;
    String correctFormDevanagari;
    UUID vocabularyWordId;
    QuestionLanguage questionSourceLanguage;
    QuestionLanguage questionTargetLanguage;
    String correctTranslationRu;
    String correctTranslationEn;
    String userLocale; // The locale for which this question was generated

    // New fields for structured question data
    String stem;
    String caseType;
    String numberType;
}
