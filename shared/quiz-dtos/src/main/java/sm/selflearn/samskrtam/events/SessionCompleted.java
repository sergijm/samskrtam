package sm.selflearn.samskrtam.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import sm.selflearn.samskrtam.content.dto.QuizType; // Corrected import

import java.util.List; // Import List
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
    List<AnswerData> answers; // New field to store the session history
}
