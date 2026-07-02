package sm.selflearn.samskrtam.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmitted extends AbstractEvent {
    UUID    userId;
    LessonType lessonType;
    UUID    lessonId; // Changed from quizId to lessonId
    UUID    questionId;
    UUID    selectedOptionId;
    Boolean isCorrect;
    int     responseTimeMs;
}

