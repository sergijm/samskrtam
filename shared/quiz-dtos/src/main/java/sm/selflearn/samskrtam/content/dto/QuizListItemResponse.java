package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;


import java.util.UUID;

@Value
@Builder
@Jacksonized
public class QuizListItemResponse {
    UUID id;
    String title; // Keep existing title for backward compatibility or default
    String titleRu; // New field for Russian title
    String titleEn; // New field for English title
    String description; // Keep existing description for backward compatibility or default
    String descriptionRu; // New field for Russian description
    String descriptionEn; // New field for English description
    LessonType lessonType;
    String slug;
    int totalQuestions;
}
