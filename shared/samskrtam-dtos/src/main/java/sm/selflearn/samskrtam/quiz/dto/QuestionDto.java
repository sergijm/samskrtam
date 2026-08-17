package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.quest.AnswerMode;
import sm.selflearn.samskrtam.quest.HighlightToken;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class QuestionDto {
    UUID id;
    int questionNumber;
    String text;

    /**
     * Russian variant of {@link #text} for bilingual questions (curriculum compose flow).
     * Null for legacy content-based questions and when no Russian variant is available.
     */
    String textRu;
    List<QuestionOptionDto> options;

    // Existing structured question fields
    String stem;
    String caseType;
    String numberType;
    String gender;
    String stemDevanagari;
    String stemTranslationRu;
    String stemTranslationEn;

    /**
     * Question type. When null, frontend treats as "FORM_BY_CASE".
     * Supported: null/"FORM_BY_CASE", "CASE_BY_FORM", "MULTIPLE_CHOICE".
     */
    String questionType;

    /**
     * Answer mode (curriculum compose flow): FREE_TEXT, SINGLE_CHOICE, MATCHING.
     * Null for legacy content-based sessions — the frontend dispatches on
     * {@link #questionType} in that case.
     */
    AnswerMode answerMode;

    /** Whether this question supports multi-select (e.g. CASE_BY_FORM). Default false. */
    boolean multiSelect;

    /** Prompt form (IAST) for CASE_BY_FORM / MULTIPLE_CHOICE — the form the user must identify. */
    String formIast;

    /** Prompt form (Devanagari) for CASE_BY_FORM / MULTIPLE_CHOICE. */
    String formDevanagari;

    /** Case ending string for reference display. */
    String caseEnding;

    /**
     * Left-side rows of a MATCHING question (questionType = "MATCHING"). The right-side
     * labels are carried in {@code options} with optionType = "MATCH_LABEL". Null/empty
     * for non-matching questions.
     */
    List<QuestionMatchRowDto> matchRows;

    /**
     * Words of the prompt to highlight (bilingual: English/Russian variants).
     * Populated by the curriculum compose flow; null for legacy questions.
     */
    List<HighlightToken> highlights;
}

