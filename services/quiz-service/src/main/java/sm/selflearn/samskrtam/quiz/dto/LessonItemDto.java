package sm.selflearn.samskrtam.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import sm.selflearn.samskrtam.content.dto.Difficulty;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonItemDto {
    // === Данные от content-service ===
    private UUID id;
    private String slug;
    private String titleRu;
    private String titleEn;
    private String descriptionRu;
    private String descriptionEn;
    private LessonType lessonType;
    private Difficulty difficulty;
    private int totalQuestions;

    // === Поля прогресса (для VOCABULARY) ===
    private int totalWordsOwn;    // wordCount из content-service (слова без подкатегорий)
    private int learnedWords;     // слова userId со score >= 80
}
