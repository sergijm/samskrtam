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
}