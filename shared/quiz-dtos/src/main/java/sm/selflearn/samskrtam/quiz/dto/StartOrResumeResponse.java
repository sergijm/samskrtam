package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.LessonType; // Corrected import

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class StartOrResumeResponse {
    UUID sessionId;
    UUID quizId;
    LessonType lessonType;
    List<QuestionDto> questions;
    int totalQuestions;
    int answeredQuestions;
    int score;
    int currentQuestionIndex;
    int currentQuestionNumber; // New field
    String quizTitleRu; // New field
    String quizTitleEn; // New field
    String quizDescriptionRu; // New field
    String quizDescriptionEn; // New field
    String slug; // New field
}
