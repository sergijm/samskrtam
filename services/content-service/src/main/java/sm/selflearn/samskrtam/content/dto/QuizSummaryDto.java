package sm.selflearn.samskrtam.content.dto;

import lombok.Data;
import sm.selflearn.samskrtam.content.model.Difficulty;
import sm.selflearn.samskrtam.content.model.QuizType;
import java.util.UUID;

@Data
public class QuizSummaryDto {
    private UUID id;
    private String slug;
    private String titleRu;
    private String titleEn;
    private QuizType quizType;
    private Difficulty difficulty;
}
