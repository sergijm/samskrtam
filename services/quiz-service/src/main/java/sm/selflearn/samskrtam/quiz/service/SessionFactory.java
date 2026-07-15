package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.model.FilterScope;
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
        List<VocabularyWordDto> vocabularyWords = generatedQuizData.getVocabularyWords() != null ? generatedQuizData.getVocabularyWords() : Collections.emptyList();
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
     * Creates a new filtered quiz session with filter scope using JSONB sets.
     * See docs/services/quiz-service/quiz-declension.md §3.4
     *
     * @param filterCaseTypes JSON array string for CASE_ONLY
     * @param filterNumberTypes JSON array string for NUMBER_ONLY
     * @param filterCombinations JSON array string for CASE_NUMBER_GENDER
     */
    public QuizSession createFilteredSession(UUID lessonId, UUID userId, GeneratedQuizData generatedQuizData,
                                               FilterScope filterScope, String filterCaseTypes,
                                               String filterNumberTypes, String filterCombinations) {
        List<VocabularyWordDto> vocabularyWords = generatedQuizData.getVocabularyWords() != null ? generatedQuizData.getVocabularyWords() : Collections.emptyList();
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
                .filterScope(filterScope)
                .filterCaseTypes(filterCaseTypes)
                .filterNumberTypes(filterNumberTypes)
                .filterCombinations(filterCombinations)
                .build();
    }
}

