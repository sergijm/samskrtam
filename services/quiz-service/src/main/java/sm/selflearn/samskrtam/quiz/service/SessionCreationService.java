package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.mapper.SessionQuestionMapper;
import sm.selflearn.samskrtam.quiz.model.FilterScope;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.model.StatusFilter;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * Отвечает за создание новых квиз-сессий всех видов (plain, filter-scoped, status-filtered).
 * Выделен из QuizSessionService для соблюдения компактности файлов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCreationService {

    private final QuizSessionRepository quizSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final ContentClient contentClient;
    private final SessionFactory sessionFactory;
    private final SessionQuestionMapper sessionQuestionMapper;
    private final SessionPublisher sessionPublisher;
    private final QuizDataAssembler quizDataAssembler;

    // ================== Plain session ==================

    @Transactional
    public Mono<sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse> createNewSession(
            UUID lessonId, UUID userId, String userLocale) {
        return contentClient.generateQuizData(lessonId, userLocale)
                .flatMap(generatedQuizData -> {
                    QuizSession newSession = sessionFactory.createSession(lessonId, userId, generatedQuizData);
                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
                                List<SessionQuestion> sessionQuestions = generatedQuizData.getGeneratedQuestions()
                                        .stream()
                                        .map(q -> sessionQuestionMapper.fromDto(q, savedSession.getId()))
                                        .collect(Collectors.toList());
                                return sessionQuestionRepository.saveAll(sessionQuestions)
                                        .then(sessionPublisher.publishStarted(savedSession))
                                        .then(quizDataAssembler.assembleResponse(
                                                savedSession,
                                                generatedQuizData.getGeneratedQuestions(),
                                                generatedQuizData.getVocabularyWords(),
                                                userLocale));
                            });
                });
    }

    // ================== Filter-scoped session ==================

        @Transactional
        public Mono<sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse> createFilteredSession(
            UUID lessonId, UUID userId, String userLocale,
            FilterScope filterScope, String filterCaseTypes,
            String filterNumberTypes, String filterCombinations,
            String filterVowelTypes, String filterGenders) {
        String filterScopeStr = filterScope != null ? filterScope.name() : null;
        return contentClient.generateQuizData(lessonId, userLocale,
                        filterScopeStr, filterCaseTypes, filterNumberTypes, filterCombinations,
                        filterVowelTypes, filterGenders)
                .flatMap(generatedQuizData -> {
                    QuizSession newSession = sessionFactory.createFilteredSession(
                            lessonId, userId, generatedQuizData,
                            filterScope, filterCaseTypes, filterNumberTypes, filterCombinations,
                            filterVowelTypes, filterGenders);
                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
                                List<GeneratedQuizQuestionDto> allQuestions = generatedQuizData.getGeneratedQuestions();
                                if (allQuestions.isEmpty()) {
                                    return Mono.error(new SamskrtamException("SCOPE_FILTER_EMPTY",
                                            "No questions match the filter scope: " + filterScope
                                            + " filterCaseTypes=" + filterCaseTypes
                                            + " filterNumberTypes=" + filterNumberTypes
                                            + " filterCombinations=" + filterCombinations
                                            + " filterVowelTypes=" + filterVowelTypes
                                            + " filterGenders=" + filterGenders));
                                }
                                return saveAllAndRespond(savedSession, allQuestions,
                                        generatedQuizData, userLocale);
                            });
                });
    }

    // ================== Status-filtered session ==================

    /**
     * Create a new session with statusFilter (§3, §4 п.«2а»).
     * If the bucket pool is empty → 404 (not 200 with empty questions).
     */
    @Transactional
    public Mono<sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse> createStatusFilteredSession(
            UUID lessonId, UUID userId, String userLocale,
            StatusFilter statusFilter) {
        return contentClient.generateQuizData(lessonId, userLocale)
                .flatMap(generatedQuizData -> {
                    QuizSession newSession = sessionFactory.createStatusFilteredSession(
                            lessonId, userId, generatedQuizData, statusFilter);
                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
                                List<GeneratedQuizQuestionDto> allQuestions = generatedQuizData.getGeneratedQuestions();
                                return saveAllAndRespond(savedSession, allQuestions,
                                        generatedQuizData, userLocale);
                            });
                });
    }

    // ================== Internal helpers ==================

    private Mono<sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse> saveAllAndRespond(
            QuizSession savedSession,
            List<GeneratedQuizQuestionDto> allQuestions,
            sm.selflearn.samskrtam.content.dto.GeneratedQuizData generatedQuizData,
            String userLocale) {
        List<SessionQuestion> sessionQuestions = allQuestions.stream()
                .map(q -> sessionQuestionMapper.fromDto(q, savedSession.getId()))
                .collect(Collectors.toList());
        return sessionQuestionRepository.saveAll(sessionQuestions)
                .then(sessionPublisher.publishStarted(savedSession))
                .then(quizDataAssembler.assembleResponse(
                        savedSession, allQuestions,
                        generatedQuizData.getVocabularyWords(), userLocale));
    }
}
