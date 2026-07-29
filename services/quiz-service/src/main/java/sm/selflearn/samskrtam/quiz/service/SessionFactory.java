package sm.selflearn.samskrtam.quiz.service;

import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizData;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.model.FilterScope;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.model.StatusFilter;

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
     * See docs/services/quiz-service/quiz-declension.md §3.4, §5.3
     *
     * @param filterCaseTypes JSON array string for CASE_ONLY
     * @param filterNumberTypes JSON array string for NUMBER_ONLY
     * @param filterCombinations JSON array string for CASE_NUMBER_GENDER
     * @param filterVowelTypes JSON array string for ALL_STEMS
     * @param filterGenders JSON array string for ALL_STEMS
     */
    public QuizSession createFilteredSession(UUID lessonId, UUID userId, GeneratedQuizData generatedQuizData,
                                               FilterScope filterScope, String filterCaseTypes,
                                               String filterNumberTypes, String filterCombinations,
                                               String filterVowelTypes, String filterGenders) {
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
                .filterCaseTypes(filterCaseTypes != null ? Json.of(filterCaseTypes) : null)
                .filterNumberTypes(filterNumberTypes != null ? Json.of(filterNumberTypes) : null)
                .filterCombinations(filterCombinations != null ? Json.of(filterCombinations) : null)
                .filterVowelTypes(filterVowelTypes != null ? Json.of(filterVowelTypes) : null)
                .filterGenders(filterGenders != null ? Json.of(filterGenders) : null)
                .build();
    }

    /**
     * Creates a new quiz session with a status filter (NEW|LEARNING|REVIEW).
     * See docs/services/quiz-service/quiz-generator-spec.md §3 (statusFilter).
     */
    public QuizSession createStatusFilteredSession(UUID lessonId, UUID userId,
                                                    GeneratedQuizData generatedQuizData,
                                                    StatusFilter statusFilter) {
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
                .statusFilter(statusFilter)
                .build();
    }
}

