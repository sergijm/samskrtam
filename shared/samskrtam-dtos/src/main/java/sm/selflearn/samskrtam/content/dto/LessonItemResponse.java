package sm.selflearn.samskrtam.content.dto;

import lombok.AllArgsConstructor;import lombok.Builder;
import lombok.Data;import lombok.NoArgsConstructor;import lombok.extern.jackson.Jacksonized;

import java.util.UUID;

@Data
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class LessonItemResponse {
    private UUID id;
    private String title;
    private String titleRu;
    private String titleEn;
    private String description;
    private String descriptionRu;
    private String descriptionEn;
    private LessonType lessonType;
    private Difficulty difficulty;
    private String slug;
    private int totalQuestions;
    private int wordCount;
}