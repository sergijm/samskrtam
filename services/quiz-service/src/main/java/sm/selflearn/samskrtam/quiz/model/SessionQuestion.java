package sm.selflearn.samskrtam.quiz.model;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import sm.selflearn.samskrtam.content.model.Case;
import sm.selflearn.samskrtam.content.model.Number; // Corrected import for Number enum

import java.util.UUID;

@Data
@Builder
@Table(name = "session_questions", schema = "quiz")
public class SessionQuestion {
    @Id
    private UUID id; // Primary key for this session question record
    private UUID sessionId; // Foreign key to QuizSession
    private UUID questionId; // Original ID of the question from content service
    private int questionNumber; // New field for the order of the question in the session
    private String text;
    private String explanationRu;
    private String explanationEn;
    private UUID declensionStemId;
    private Case targetCase;
    private Number targetNumber; // Now correctly refers to sm.selflearn.samskrtam.content.model.Number
    private String correctFormIast;
    private String correctFormDevanagari;
    private UUID vocabularyWordId;
    private QuestionLanguage questionSourceLanguage;
    private QuestionLanguage questionTargetLanguage;
    private String correctTranslationRu;
    private String correctTranslationEn;
}
