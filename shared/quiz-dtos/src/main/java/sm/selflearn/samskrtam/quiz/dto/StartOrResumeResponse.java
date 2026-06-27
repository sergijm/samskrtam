package sm.selflearn.samskrtam.quiz.dto;

import lombok.Builder;
import lombok.Value;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class StartOrResumeResponse {
    UUID sessionId;
    UUID lessonId;
    LessonType lessonType;
    List<QuestionDto> questions;
    int totalQuestions;
    int answeredQuestions;
    int score;
    int currentQuestionIndex;
    int currentQuestionNumber;
    String lessonTitleRu;
    String lessonTitleEn;
    String lessonDescriptionRu;
    String lessonDescriptionEn;
    String slug;
}

