package sm.selflearn.samskrtam.content.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sm.selflearn.samskrtam.content.dto.QuizType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "generated_quiz_data", schema = "content")
public class GeneratedQuizDataRecord {
    @Id
    private UUID id; // This will be our generatedQuizDataId

    @Column(nullable = false)
    private UUID quizId;

    // Removed quizType field as it can be joined from the Quiz entity

    @Column(nullable = false)
    private String userLocale;

    @Column(nullable = false)
    private Instant generatedAt;

    @Column(columnDefinition = "TEXT")
    private String vocabularyWordsJson; // To store vocabulary words for VOCABULARY quizzes
}
