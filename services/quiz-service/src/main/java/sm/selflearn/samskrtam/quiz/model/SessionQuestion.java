package sm.selflearn.samskrtam.quiz.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Data
@Builder
@Table(name = "session_questions", schema = "quiz")
public class SessionQuestion {

    @Id
    private UUID id;
    private UUID sessionId;
    private UUID questionId;
    private int questionNumber;
    private String text;
    private String explanationRu;
    private String explanationEn;
    private UUID declensionStemId;
    private String stem; // Renamed 'stemDevanagari' to 'stem' for clarity
    private String stemDevanagari;
    private String stemTranslationRu;
    private String stemTranslationEn;
    private String targetCase;
    private String targetNumber;
    private String targetGender;
    private String correctFormIast;
    private String correctFormDevanagari;
    private UUID vocabularyWordId;
    private String questionSourceLanguage;
    private String questionTargetLanguage;
    private String correctTranslationRu;
    private String correctTranslationEn;
    private String questionType;

    /**
     * ID соответствующего case_ending (для DECLENSION_FORM).
     * Копируется из GeneratedQuizQuestionDto.caseEndingId при старте сессии.
     */
    private UUID caseEndingId;

    /**
     * Тип элемента квиза (VOCABULARY_WORD, DECLENSION_FORM, ...).
     * Копируется из GeneratedQuizQuestionDto.itemType при старте сессии.
     */
    private String itemType;

    /**
     * Case ending string (e.g. "aḥ", "am", "ena") for ENDING_MATCH / CASE_BY_FORM display.
     * Копируется из GeneratedQuizQuestionDto.caseEnding при старте сессии.
     */
    private String caseEnding;
}