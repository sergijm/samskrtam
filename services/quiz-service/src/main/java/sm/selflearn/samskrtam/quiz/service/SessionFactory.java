package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionFactory {

    private final VocabularyWordsSerializer vocabularyWordsSerializer;

    public QuizSession createSession(UUID lessonId, UUID userId, GeneratedQuizData generatedQuizData) {
        String vocabularyWordsJson = vocabularyWordsSerializer.serialize(generatedQuizData.getVocabularyWords());

        return QuizSession.builder()
                .id(null)
                .userId(userId)
                .lessonId(lessonId)
                .lessonType(generatedQuizData.getLessonType())
                .totalQuestions(generatedQuizData.getQuestionsPerSession())
                .answeredQuestions(0)
                .score(0)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .vocabularyWordsJson(vocabularyWordsJson)
                .generatedQuizDataId(generatedQuizData.getGeneratedQuizDataId())
                .build();
    }
}