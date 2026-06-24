package sm.selflearn.samskrtam.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSummaryDto {
    private UUID id;
    private String slug;
    private String titleRu;
    private String titleEn;
    private String descriptionRu; // New field
    private String descriptionEn; // New field
    private LessonType lessonType;
    private Difficulty difficulty;
}
