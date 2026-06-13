package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Data;
import sm.selflearn.samskrtam.content.dto.QuizType;

import java.util.UUID;

@Data
@Builder
public class QuizListItemResponse {
    private UUID id;
    private String title;
    private String titleRu;
    private String titleEn;
    private String description;
    private String descriptionRu;
    private String descriptionEn;
    private QuizType quizType;
    private String slug;
    private int totalQuestions;
    private int wordCount; // New field for word count
}
