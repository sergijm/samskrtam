package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.QuizType; // Corrected import

import java.util.UUID;

@Value
@Builder
public class QuizListItemResponse {
    UUID id;
    String title;
    String titleRu;
    String titleEn;
    String description;
    String descriptionRu;
    String descriptionEn;
    QuizType quizType;
    String slug;
    int totalQuestions;
}
