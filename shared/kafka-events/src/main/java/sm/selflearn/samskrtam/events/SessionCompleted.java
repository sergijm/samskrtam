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
public class SessionCompleted extends AbstractEvent {
    UUID    userId;
    QuizType quizType;
    UUID    quizId;
    int     score;
    int     totalQuestions;
    long    durationMs;
}
