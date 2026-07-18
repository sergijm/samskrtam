package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class QuestionDto {
    UUID id;
    int questionNumber;
    String text;
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

    /** Whether this question supports multi-select (e.g. CASE_BY_FORM). Default false. */
    boolean multiSelect;

    /** Prompt form (IAST) for CASE_BY_FORM / MULTIPLE_CHOICE — the form the user must identify. */
    String formIast;

    /** Prompt form (Devanagari) for CASE_BY_FORM / MULTIPLE_CHOICE. */
    String formDevanagari;

    /** Case ending string for reference display. */
    String caseEnding;
}

