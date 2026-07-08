package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.AnswerRequest;
import sm.selflearn.samskrtam.quiz.dto.AnswerResponse;
import sm.selflearn.samskrtam.quiz.dto.CompleteSessionResponse;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.model.FilterScope;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItem;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;
import sm.selflearn.samskrtam.quiz.mapper.SessionQuestionMapper;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис управления квиз-сессиями.
 * Содержит публичные API-методы; вся сложная логика делегирована в {@link SessionOperationsService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final ContentClient contentClient;
    private final QuizDataAssembler quizDataAssembler;
    private final SessionFactory sessionFactory;
    private final SessionQuestionRepository sessionQuestionRepository;
    private final SessionQuestionMapper sessionQuestionMapper;
    private final SessionPublisher sessionPublisher;

    private final QuizGenerator quizGenerator;
    private final SessionOperationsService sessionOperationsService;

    public Mono<StartOrResumeResponse> startOrResumeSession(UUID lessonId, UUID userId, String userLocale) {
        return quizSessionRepository
                .findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(userId, lessonId, SessionStatus.IN_PROGRESS)
                .flatMap(session -> sessionOperationsService.resume(session, userLocale))
                .switchIfEmpty(Mono.defer(() -> createNewSession(lessonId, userId, userLocale)));
    }

    public Mono<StartOrResumeResponse> startOrResumeSession(
            UUID lessonId, UUID userId, String userLocale,
            FilterScope filterScope, String filterCaseType,
            String filterNumberType, String filterGender) {
        if (filterScope == null) {
            return startOrResumeSession(lessonId, userId, userLocale);
        }
        String scope = filterScope.name();
        String numberType = (filterScope == FilterScope.CASE_NUMBER_GENDER) ? filterNumberType : null;
        String gender = (filterScope == FilterScope.CASE_NUMBER_GENDER) ? filterGender : null;
        return quizSessionRepository
                .findInProgressByFilter(userId, lessonId, scope, filterCaseType, numberType, gender)
                .flatMap(session -> sessionOperationsService.resume(session, userLocale))
                .switchIfEmpty(Mono.defer(() -> createFilteredSession(lessonId, userId, userLocale,
                        filterScope, filterCaseType, numberType, gender)));
    }

    public Mono<StartOrResumeResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionOperationsService.resume(session, userLocale));
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionOperationsService.submitAnswer(session, userId, request, userLocale));
    }

    public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(sessionOperationsService::completeSession);
    }

    public Mono<StartOrResumeResponse> retakeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionOperationsService.retakeSession(session, userLocale));
    }

    public Mono<StartOrResumeResponse> startNewQuizFromExistingSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> sessionOperationsService.startNewQuizFromExistingSession(session)
                        .flatMap(completedSession ->
                                createNewSession(completedSession.getLessonId(), userId, userLocale)));
    }

    private Mono<StartOrResumeResponse> createNewSession(UUID lessonId, UUID userId, String userLocale) {
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

    private Mono<StartOrResumeResponse> createFilteredSession(
            UUID lessonId, UUID userId, String userLocale,
            FilterScope filterScope, String filterCaseType,
            String filterNumberType, String filterGender) {
        // 1. Получаем все вопросы урока
        return contentClient.generateQuizData(lessonId, userLocale)
                .flatMap(generatedQuizData -> {
                    QuizSession newSession = sessionFactory.createFilteredSession(
                            lessonId, userId, generatedQuizData,
                            filterScope, filterCaseType, filterNumberType, filterGender);
                    return quizSessionRepository.save(newSession)
                            .flatMap(savedSession -> {
                                List<GeneratedQuizQuestionDto> allQuestions = generatedQuizData.getGeneratedQuestions();

                                // Определяем itemType по явному полю DTO
                                ItemType itemType = resolveItemTypeFromQuestions(allQuestions, savedSession.getId());
                                if (itemType == null) {
                                    // Неизвестный тип или null — сохраняем все вопросы как есть
                                    List<SessionQuestion> sessionQuestions = allQuestions.stream()
                                            .map(q -> sessionQuestionMapper.fromDto(q, savedSession.getId()))
                                            .collect(Collectors.toList());
                                    return sessionQuestionRepository.saveAll(sessionQuestions)
                                            .then(sessionPublisher.publishStarted(savedSession))
                                            .then(quizDataAssembler.assembleResponse(
                                                    savedSession, allQuestions,
                                                    generatedQuizData.getVocabularyWords(), userLocale));
                                }

                                // Собираем externalRefIds по itemType
                                List<UUID> externalRefIds = allQuestions.stream()
                                        .map(q -> switch (itemType) {
                                            case DECLENSION_FORM -> q.getCaseEndingId();
                                            case VOCABULARY_WORD -> q.getVocabularyWordId();
                                        })
                                        .filter(id -> id != null)
                                        .collect(Collectors.toList());

                                if (externalRefIds.isEmpty()) {
                                    List<SessionQuestion> sessionQuestions = allQuestions.stream()
                                            .map(q -> sessionQuestionMapper.fromDto(q, savedSession.getId()))
                                            .collect(Collectors.toList());
                                    return sessionQuestionRepository.saveAll(sessionQuestions)
                                            .then(sessionPublisher.publishStarted(savedSession))
                                            .then(quizDataAssembler.assembleResponse(
                                                    savedSession, allQuestions,
                                                    generatedQuizData.getVocabularyWords(), userLocale));
                                }

                                // Фильтруем вопросы через QuizGenerator
                                return quizGenerator.generate(userId, itemType, externalRefIds)
                                        .map(selectedItems -> {
                                            Set<UUID> selectedRefIds = selectedItems.stream()
                                                    .map(QuizItem::externalRefId)
                                                    .collect(Collectors.toSet());
                                            return allQuestions.stream()
                                                    .filter(q -> {
                                                        UUID refId = switch (itemType) {
                                                            case DECLENSION_FORM -> q.getCaseEndingId();
                                                            case VOCABULARY_WORD -> q.getVocabularyWordId();
                                                        };
                                                        return refId != null && selectedRefIds.contains(refId);
                                                    })
                                                    .collect(Collectors.toList());
                                        })
                                        .flatMap(filteredQuestions -> {
                                            List<SessionQuestion> sessionQuestions = filteredQuestions.stream()
                                                    .map(q -> sessionQuestionMapper.fromDto(q, savedSession.getId()))
                                                    .collect(Collectors.toList());
                                            return sessionQuestionRepository.saveAll(sessionQuestions)
                                                    .then(sessionPublisher.publishStarted(savedSession))
                                                    .then(quizDataAssembler.assembleResponse(
                                                            savedSession, filteredQuestions,
                                                            generatedQuizData.getVocabularyWords(), userLocale));
                                        });
                            });
                });
    }

    /**
     * Определяет ItemType по явному полю itemType в DTO вопросов.
     * Если поле отсутствует — fallback-эвристика для обратной совместимости.
     */
    private ItemType resolveItemTypeFromQuestions(List<GeneratedQuizQuestionDto> questions, UUID sessionId) {
        if (questions.isEmpty()) {
            log.warn("No questions in session {}, cannot resolve itemType", sessionId);
            return null;
        }
        GeneratedQuizQuestionDto first = questions.get(0);
        if (first.getItemType() != null) {
            try {
                return ItemType.valueOf(first.getItemType());
            } catch (IllegalArgumentException e) {
                log.warn("Unknown itemType '{}' in session {}, falling back to heuristics",
                        first.getItemType(), sessionId);
            }
        }
        // Fallback для обратной совместимости (пока content-service не обновлён)
        if (first.getVocabularyWordId() != null) return ItemType.VOCABULARY_WORD;
        if (first.getCaseEndingId() != null) return ItemType.DECLENSION_FORM;
        log.error("Cannot resolve ItemType for session {}: no itemType and no known Id field", sessionId);
        return null;
    }
}
