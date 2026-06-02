package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import sm.selflearn.samskrtam.content.model.QuizType;

import java.util.UUID;

@Value
@Builder
@Jacksonized
public class QuizListItemResponse {
    UUID id;
    String title;
    String description;
    QuizType quizType;
    String slug;
    int totalQuestions;
}
