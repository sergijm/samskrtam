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
import sm.selflearn.samskrtam.quiz.model.StatusFilter;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;
import sm.selflearn.samskrtam.quiz.mapper.SessionQuestionMapper;

import java.util.ArrayList;
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

        /**
     * Unified dispatcher: routes to plain, filter-scoped, or status-filtered branch.
     */
    public Mono<StartOrResumeResponse> startOrResumeSession(
            UUID lessonId, UUID userId, String userLocale,
            FilterScope filterScope, String filterCaseTypes,
            String filterNumberTypes, String filterCombinations,
            StatusFilter statusFilter) {
        if (statusFilter != null) {
            return startOrResumeWithStatusFilter(lessonId, userId, userLocale, statusFilter);
        }
        if (filterScope != null) {
            return startOrResumeWithFilterScope(lessonId, userId, userLocale,
                    filterScope, filterCaseTypes, filterNumberTypes, filterCombinations);
        }
        return startOrResumeSession(lessonId, userId, userLocale);
    }

    /** Plain start-or-resume (no filterScope, no statusFilter). */
    public Mono<StartOrResumeResponse> startOrResumeSession(UUID lessonId, UUID userId, String userLocale) {
        return quizSessionRepository
                .findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(userId, lessonId, SessionStatus.IN_PROGRESS)
                .flatMap(session -> sessionOperationsService.resume(session, userLocale))
                .switchIfEmpty(Mono.defer(() -> createNewSession(lessonId, userId, userLocale)));
    }

    private Mono<StartOrResumeResponse> startOrResumeWithFilterScope(
            UUID lessonId, UUID userId, String userLocale,
            FilterScope filterScope, String filterCaseTypes,
            String filterNumberTypes, String filterCombinations) {
        String scope = filterScope.name();
        String canonicalCaseTypes = null;
        String canonicalNumberTypes = null;
        String canonicalCombinations = null;

        switch (filterScope) {
            case CASE_ONLY -> canonicalCaseTypes = buildCanonicalJsonArray(
                    parseCsvToList(filterCaseTypes));
            case NUMBER_ONLY -> canonicalNumberTypes = buildCanonicalJsonArray(
                    parseCsvToList(filterNumberTypes));
            case CASE_NUMBER_GENDER -> canonicalCombinations = buildCanonicalCombinationsJson(
                    parseCombinations(filterCombinations));
        }

        final String finalCaseTypes = canonicalCaseTypes;
        final String finalNumberTypes = canonicalNumberTypes;
        final String finalCombinations = canonicalCombinations;

        return quizSessionRepository
                .findInProgressByFilter(userId, lessonId, scope,
                        finalCaseTypes, finalNumberTypes, finalCombinations)
                .flatMap(session -> sessionOperationsService.resume(session, userLocale))
                .switchIfEmpty(Mono.defer(() -> createFilteredSession(lessonId, userId, userLocale,
                        filterScope, finalCaseTypes, finalNumberTypes, finalCombinations)));
    }

    /**
     * Status-filter branch (§3, §4 п.«2а»):
     * NEW → все единицы без строки score;
     * LEARNING → строка есть, бакет LEARNING/DIFFICULT;
     * REVIEW → findDueItems (MASTERED, due).
     */
    private Mono<StartOrResumeResponse> startOrResumeWithStatusFilter(
            UUID lessonId, UUID userId, String userLocale,
            StatusFilter statusFilter) {
        String statusFilterStr = statusFilter.name();
        return quizSessionRepository
                .findInProgressByStatusFilter(userId, lessonId, statusFilterStr)
                .flatMap(session -> sessionOperationsService.resume(session, userLocale))
                .switchIfEmpty(Mono.defer(() -> createStatusFilteredSession(
                        lessonId, userId, userLocale, statusFilter)));
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
                FilterScope filterScope, String filterCaseTypes,
                String filterNumberTypes, String filterCombinations) {
            // 1. Получаем все вопросы урока
            return contentClient.generateQuizData(lessonId, userLocale)
                    .flatMap(generatedQuizData -> {
                        QuizSession newSession = sessionFactory.createFilteredSession(
                                lessonId, userId, generatedQuizData,
                                filterScope, filterCaseTypes, filterNumberTypes, filterCombinations);
                        return quizSessionRepository.save(newSession)
                                .flatMap(savedSession -> {
                                    List<GeneratedQuizQuestionDto> allQuestions = generatedQuizData.getGeneratedQuestions();
                                    return filterAndSaveQuestions(savedSession, allQuestions,
                                            generatedQuizData, userId, userLocale, false);
                                });
                    });
        }

        /**
         * Create a new session with statusFilter (§3, §4 п.«2а»).
         * If the bucket pool is empty → 404 (not 200 with empty questions).
         */
        private Mono<StartOrResumeResponse> createStatusFilteredSession(
                UUID lessonId, UUID userId, String userLocale,
                StatusFilter statusFilter) {
            return contentClient.generateQuizData(lessonId, userLocale)
                    .flatMap(generatedQuizData -> {
                        QuizSession newSession = sessionFactory.createStatusFilteredSession(
                                lessonId, userId, generatedQuizData, statusFilter);
                        return quizSessionRepository.save(newSession)
                                .flatMap(savedSession -> {
                                    List<GeneratedQuizQuestionDto> allQuestions = generatedQuizData.getGeneratedQuestions();
                                    return filterAndSaveQuestions(savedSession, allQuestions,
                                            generatedQuizData, userId, userLocale, true);
                                });
                    });
        }

        /**
         * Common question filtering+save logic extracted from createFilteredSession/createStatusFilteredSession.
         *
         * @param statusFiltered if true, uses generateStatusFiltered; if false, uses plain generate
         */
        private Mono<StartOrResumeResponse> filterAndSaveQuestions(
                QuizSession savedSession,
                List<GeneratedQuizQuestionDto> allQuestions,
                sm.selflearn.samskrtam.content.dto.GeneratedQuizData generatedQuizData,
                UUID userId, String userLocale,
                boolean statusFiltered) {

            ItemType itemType = resolveItemTypeFromQuestions(allQuestions, savedSession.getId());
            if (itemType == null) {
                List<SessionQuestion> sessionQuestions = allQuestions.stream()
                        .map(q -> sessionQuestionMapper.fromDto(q, savedSession.getId()))
                        .collect(Collectors.toList());
                return sessionQuestionRepository.saveAll(sessionQuestions)
                        .then(sessionPublisher.publishStarted(savedSession))
                        .then(quizDataAssembler.assembleResponse(
                                savedSession, allQuestions,
                                generatedQuizData.getVocabularyWords(), userLocale));
            }

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

            // Choose generator branch
            Mono<List<QuizItem>> selectedItemsMono;
            if (statusFiltered) {
                selectedItemsMono = quizGenerator.generateStatusFiltered(
                        userId, itemType, externalRefIds, savedSession.getStatusFilter());
            } else {
                selectedItemsMono = quizGenerator.generate(userId, itemType, externalRefIds);
            }

            return selectedItemsMono
                    .flatMap(selectedItems -> {
                        if (selectedItems.isEmpty()) {
                            // Empty pool for statusFilter → 404
                            return Mono.error(new SamskrtamException("STATUS_FILTER_POOL_EMPTY",
                                    "No items available for statusFilter=" + savedSession.getStatusFilter()));
                        }
                        Set<UUID> selectedRefIds = selectedItems.stream()
                                .map(QuizItem::externalRefId)
                                .collect(Collectors.toSet());
                        List<GeneratedQuizQuestionDto> filteredQuestions = allQuestions.stream()
                                .filter(q -> {
                                    UUID refId = switch (itemType) {
                                        case DECLENSION_FORM -> q.getCaseEndingId();
                                        case VOCABULARY_WORD -> q.getVocabularyWordId();
                                    };
                                    return refId != null && selectedRefIds.contains(refId);
                                })
                                .collect(Collectors.toList());
                        List<SessionQuestion> sessionQuestions = filteredQuestions.stream()
                                .map(q -> sessionQuestionMapper.fromDto(q, savedSession.getId()))
                                .collect(Collectors.toList());
                        return sessionQuestionRepository.saveAll(sessionQuestions)
                                .then(sessionPublisher.publishStarted(savedSession))
                                .then(quizDataAssembler.assembleResponse(
                                        savedSession, filteredQuestions,
                                        generatedQuizData.getVocabularyWords(), userLocale));
                    });
        }

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
        // Fallback для обратной совместимости
        if (first.getVocabularyWordId() != null) return ItemType.VOCABULARY_WORD;
        if (first.getCaseEndingId() != null) return ItemType.DECLENSION_FORM;
        log.error("Cannot resolve ItemType for session {}: no itemType and no known Id field", sessionId);
        return null;
    }

    // ================== JSON Helpers ==================

    /** Builds a canonical sorted JSON array from a list of strings for set equality comparison. */
    static String buildCanonicalJsonArray(List<String> items) {
        if (items == null || items.isEmpty()) return null;
        List<String> sorted = new ArrayList<>(items);
        sorted.sort(String::compareTo);
        return "[" + sorted.stream()
                .map(s -> "\"" + escapeJson(s) + "\"")
                .collect(Collectors.joining(",")) + "]";
    }

    /** Builds a canonical sorted JSON array of {caseType,numberType,gender} objects. */
    static String buildCanonicalCombinationsJson(List<FilterCombination> combinations) {
        if (combinations == null || combinations.isEmpty()) return null;
        List<FilterCombination> sorted = new ArrayList<>(combinations);
        sorted.sort(FilterCombination::compareTo);
        return "[" + sorted.stream()
                .map(c -> "{\"caseType\":\"" + escapeJson(c.caseType) + "\"," +
                          "\"numberType\":\"" + escapeJson(c.numberType) + "\"," +
                          "\"gender\":\"" + escapeJson(c.gender) + "\"}")
                .collect(Collectors.joining(",")) + "]";
    }

    static List<String> parseCsvToList(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        return List.of(csv.split(","));
    }

    /** Parses comma-separated "caseType:numberType:gender" triples. */
    static List<FilterCombination> parseCombinations(String csv) {
        if (csv == null || csv.isBlank()) return List.of();
        List<FilterCombination> result = new ArrayList<>();
        for (String part : csv.split(",")) {
            String[] fields = part.split(":");
            if (fields.length >= 3) {
                result.add(new FilterCombination(fields[0].trim(), fields[1].trim(), fields[2].trim()));
            }
        }
        return result;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /** Immutable triple for filter combinations. */
    public record FilterCombination(String caseType, String numberType, String gender)
            implements Comparable<FilterCombination> {
        @Override
        public int compareTo(FilterCombination o) {
            int c = caseType.compareTo(o.caseType);
            if (c != 0) return c;
            c = numberType.compareTo(o.numberType);
            if (c != 0) return c;
            return gender.compareTo(o.gender);
        }
    }
}
