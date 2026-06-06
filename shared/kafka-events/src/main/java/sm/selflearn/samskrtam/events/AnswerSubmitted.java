package sm.selflearn.samskrtam.events;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import sm.selflearn.samskrtam.content.dto.QuizType;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerSubmitted extends AbstractEvent {
    UUID    userId;
    QuizType quizType;
    UUID    quizId;
    UUID    questionId;
    UUID    selectedOptionId;
    boolean isCorrect;
    int     responseTimeMs;
}
