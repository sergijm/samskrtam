package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.QuizType;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class ResumeSessionResponse {
    UUID sessionId;
    UUID quizId;
    QuizType quizType;
    List<QuestionDto> questions;
    int totalQuestions;
    int answeredQuestions;
    int score;
    int currentQuestionIndex;
}
