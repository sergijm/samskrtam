package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class GeneratedQuizData {
    UUID lessonId;
    LessonType lessonType;
    int questionsPerSession;
    List<GeneratedQuizQuestionDto> generatedQuestions;
    List<VocabularyWordDto> vocabularyWords;
}

