package sm.selflearn.samskrtam.quiz.repository;

import java.time.LocalDateTime;
import java.util.UUID;

public interface QuizAnswerHistoryProjection {
    UUID getId();
    Boolean getIsCorrect();
    LocalDateTime getAnsweredAt();
    String getCorrectFormIast();
    String getSelectedAnswer();
}