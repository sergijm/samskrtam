package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class QuestionOptionDto {
    UUID id;

    /**
     * Option type: "FORM" (declension form, default) or "CASE_COMBINATION" (case×number×gender triple).
     * When null, frontend treats as "FORM".
     */
    String optionType;

    /** IAST form of the declension, used when optionType = "FORM" (default). */
    String formIast;

    /**
     * Russian variant of the option text (bilingual curriculum options). Null when the
     * option text is language-neutral (e.g. a word form) or no Russian variant exists.
     */
    String textRu;

    /** Devanagari form, used when optionType = "FORM" (default). */
    String formDevanagari;

    // Fields for optionType = "CASE_COMBINATION"
    String caseType;
    String caseRu;
    String caseEn;
    String numberType;
    String numberRu;
    String numberEn;
    String gender;
    String genderRu;
    String genderEn;
}
