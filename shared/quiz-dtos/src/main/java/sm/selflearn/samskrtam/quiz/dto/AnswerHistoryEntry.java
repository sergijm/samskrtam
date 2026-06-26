package sm.selflearn.samskrtam.quiz.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerHistoryEntry {
    private LocalDateTime answeredAt;
    private String correctAnswer;
    private String userAnswer;
    private boolean isCorrect;
}
