package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized; // Import Jacksonized
import sm.selflearn.samskrtam.quiz.dto.GeneratedQuizQuestionDto; // Corrected import

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized // Add Jacksonized annotation
public class GeneratedQuizData {
    UUID generatedQuizDataId; // Renamed field
    UUID quizId;
    LessonType lessonType;
    int questionsPerSession;
    List<GeneratedQuizQuestionDto> generatedQuestions;
    List<VocabularyWordDto> vocabularyWords;
}
