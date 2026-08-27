package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.morphology.NumberType;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@Jacksonized
public class GeneratedQuizQuestionDto {
    UUID id;
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

    /**
     * ID соответствующего case_ending (для DECLENSION_FORM).
     * Позволяет QuizItemScoreUpdateStrategy корректно разрешать externalRefId.
     */
    UUID caseEndingId;

    /**
     * Тип элемента квиза (VOCABULARY_WORD, DECLENSION_FORM, ...).
     * Заполняется content-service. Позволяет quiz-service явно разрешать
     * itemType без угадывания по null-полям.
     *
     * <p>При добавлении нового ItemType:
     * <ol>
     *   <li>Добавить значение в {@code sm.selflearn.samskrtam.quiz.model.ItemType}</li>
     *   <li>Добавить заполнение этого поля в content-service</li>
     *   <li>Добавить case в resolveExternalRefId/resolveItemType в QuizItemScoreUpdateStrategy</li>
     * </ol>
     */
    String itemType;

    /**
     * Question type for rendering: FORM_BY_CASE (default/null), CASE_BY_FORM, or ENDING_MATCH.
     * Determines which option generator to use and how the frontend renders the prompt.
     * Set by content-service; null means FORM_BY_CASE for backward compatibility.
     */
    String questionType;

    /**
     * Case ending string (e.g. "aḥ", "am", "ena") for ENDING_MATCH / CASE_BY_FORM display.
     * Copied from content.case_endings.ending at generation time.
     */
    String caseEnding;
}

