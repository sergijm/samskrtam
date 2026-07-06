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
    private String stemDevanagari;       // NEW: devanagari of the stem, copied from declension_stems at session start
    private String stemTranslationRu;    // NEW: russian translation of the stem
    private String stemTranslationEn;    // NEW: english translation of the stem
    private String targetCase;
    private String targetNumber;
    private String targetGender; // NEW: gender for the question (MASCULINE, FEMININE, NEUTER, UNSPECIFIED)
    private String correctFormIast;
    private String correctFormDevanagari;
    private UUID vocabularyWordId;
    private String questionSourceLanguage;
    private String questionTargetLanguage;
    private String correctTranslationRu;
    private String correctTranslationEn;
}