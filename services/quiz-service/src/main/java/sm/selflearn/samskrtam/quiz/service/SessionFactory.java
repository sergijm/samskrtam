package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SessionFactory {

    private final VocabularyWordsSerializer vocabularyWordsSerializer;

    public QuizSession createSession(UUID lessonId, UUID userId, GeneratedQuizData generatedQuizData) {
        List<VocabularyWordDto> vocabularyWords = generatedQuizData.getVocabularyWords() != null
                ? generatedQuizData.getVocabularyWords() : Collections.emptyList();
        String vocabularyWordsJson = vocabularyWordsSerializer.serialize(vocabularyWords);

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
                .build();
    }

    /**
     * Creates a new quiz session composed from curriculum topics (universal engine).
     * No lesson-based fields apply (lessonId/lessonType stay null); the session is
     * carried purely by its {@code session_questions}, materialized at compose time.
     */
    public QuizSession createComposedSession(UUID userId, int totalQuestions) {
        return QuizSession.builder()
                .id(null)
                .userId(userId)
                .lessonId(null)
                .lessonType(null)
                .totalQuestions(totalQuestions)
                .answeredQuestions(0)
                .score(0)
                .status(SessionStatus.IN_PROGRESS)
                .startedAt(Instant.now())
                .vocabularyWordsJson(null)
                .build();
    }
}