package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class QuestionDto {
    UUID id;
    int questionNumber; // New field for the order of the question
    String text; // This will be a more general question text, not containing stem/case/number
    List<QuestionOptionDto> options;

    // New fields for structured question data
    String stem;
    String caseType;
    String numberType;

    // NEW fields: devanagari and translations of the declension stem
    String stemDevanagari;       // devanagari of the stem (from declension_stems), empty for VOCABULARY
    String stemTranslationRu;    // russian translation of the stem, empty for VOCABULARY
    String stemTranslationEn;    // english translation of the stem, empty for VOCABULARY
}

