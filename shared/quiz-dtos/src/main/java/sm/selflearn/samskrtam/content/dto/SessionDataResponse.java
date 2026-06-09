package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class SessionDataResponse {
    UUID quizId;
    QuizType quizType;
    int questionsPerSession;
    List<QuestionResponse> questions;
    List<VocabularyWordDto> vocabularyWords; // New field for vocabulary quizzes
}
