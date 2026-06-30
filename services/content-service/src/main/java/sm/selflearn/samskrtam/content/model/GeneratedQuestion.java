package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sm.selflearn.samskrtam.content.dto.QuestionLanguage;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name="generated_questions", schema = "content")
public class GeneratedQuestion {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID generatedQuizDataId; // New field to link to GeneratedQuizDataRecord

    @Column(nullable = false)
    private UUID quizId;

    @Column(nullable = false) // questionNumber cannot be null
    private int questionNumber; // New field to store the order of the question

    @Column(columnDefinition = "TEXT")
    private String text;

    @Column(columnDefinition = "TEXT")
    private String explanationRu;

    @Column(columnDefinition = "TEXT")
    private String explanationEn;

    private UUID declensionStemId;

    @Enumerated(EnumType.STRING)
    private CaseType targetCase;

    @Enumerated(EnumType.STRING)
    private NumberType targetNumber;

    private String correctFormIast;
    private String correctFormDevanagari;
    private UUID vocabularyWordId;

    @Enumerated(EnumType.STRING)
    private QuestionLanguage questionSourceLanguage;

    @Enumerated(EnumType.STRING)
    private QuestionLanguage questionTargetLanguage;

    private String correctTranslationRu;
    private String correctTranslationEn;
    private String userLocale;

    // New fields for structured question data
    @Column
    private String stem;
    @Enumerated(EnumType.STRING) // Added annotation
    @Column
    private CaseType caseType; // Changed type to Case enum
    @Enumerated(EnumType.STRING) // Added annotation
    @Column
    private NumberType numberType; // Changed type to Number enum
}
