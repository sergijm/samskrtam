package sm.selflearn.samskrtam.quiz.model;

import io.r2dbc.postgresql.codec.Json;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import sm.selflearn.samskrtam.quest.AnswerMode;

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
    private String textRu;
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

    // ===================== Curriculum-driven fields (V14) =====================

    /**
     * Curriculum answer mode (FREE_TEXT | SINGLE_CHOICE | MATCHING).
     * Non-null only for questions composed from curriculum.quest_item.
     */
    private AnswerMode answerMode;

    /**
     * Canonical answer text for curriculum questions; NULL for MATCHING.
     */
    private String correctAnswer;

    /**
     * Rendered option list as JSONB array of {"id", "text"} objects, the correct option
     * included. Fixed at session start so resume renders identical options/ids.
     */
    private Json options;

    /**
     * Materialized curriculum quest_item payload, passed through unparsed.
     */
    private Json payload;

    /**
     * curriculum.topic.code the question belongs to; NULL for legacy lesson-based questions.
     */
    private String topicCode;

    /**
     * Progress grouping tag: caseType|numberType|gender for declensions,
     * formIast/lemma for vocabulary. Used as the key in quiz_item_score.
     */
    private String progressTag;
}