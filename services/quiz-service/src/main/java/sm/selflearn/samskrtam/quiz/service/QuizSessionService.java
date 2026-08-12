package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.quiz.dto.AnswerRequest;
import sm.selflearn.samskrtam.quiz.dto.AnswerResponse;
import sm.selflearn.samskrtam.quiz.dto.CompleteSessionResponse;
import sm.selflearn.samskrtam.quiz.dto.ComposeQuizResponse;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.model.FilterScope;
import sm.selflearn.samskrtam.quiz.model.ProgressTagSetId;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.model.StatusFilter;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;

import java.util.List;
import java.util.UUID;

/**
 * Сервис управления квиз-сессиями — фасад с публичными API-методами.
 * Создание сессий делегировано в {@link SessionCreationService},
 * операции над сессиями — в {@link SessionOperationsService},
 * JSON-хелперы — в {@link QuizFilterJsonHelper}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizSessionService {

        private final QuizSessionRepository quizSessionRepository;
    private final SessionOperationsService sessionOperationsService;
    private final SessionCreationService sessionCreationService;
    private final ComposedSessionService composedSessionService;
    private final QuizComposeService quizComposeService;

    private static boolean isComposed(QuizSession session) {
        return session.getLessonId() == null;
    }

        /**
         * Unified dispatcher: routes to topic-based compose (preferred), or to the legacy
         * lesson-based plain/filter/status-filtered branches.
         */
        public Mono<StartOrResumeResponse> startOrResumeSession(
                UUID lessonId, UUID userId, String userLocale,
                FilterScope filterScope, String filterCaseTypes,
                String filterNumberTypes, String filterCombinations,
                StatusFilter statusFilter,
                String filterVowelTypes, String filterGenders) {
            if (statusFilter != null) {
                return startOrResumeWithStatusFilter(lessonId, userId, userLocale, statusFilter);
            }
            if (filterScope != null) {
                return startOrResumeWithFilterScope(lessonId, userId, userLocale,
                        filterScope, filterCaseTypes, filterNumberTypes, filterCombinations,
                        filterVowelTypes, filterGenders);
            }
            return startOrResumeSession(lessonId, userId, userLocale);
        }

    /** Plain start-or-resume (no filterScope, no statusFilter) — legacy lesson-based branch. */
    public Mono<StartOrResumeResponse> startOrResumeSession(UUID lessonId, UUID userId, String userLocale) {
        return quizSessionRepository
                .findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(userId, lessonId, SessionStatus.IN_PROGRESS)
                .flatMap(session -> sessionOperationsService.resume(session, userLocale))
                .switchIfEmpty(Mono.defer(() -> sessionCreationService.createNewSession(lessonId, userId, userLocale)));
    }

    /**
     * Topic-based start-or-resume (principle 2026-08: topics, not lessons).
     * Composes a session from a single topic via the universal engine and returns it
     * shaped as the legacy {@link StartOrResumeResponse} (lesson fields null) so the
     * front-end contract is unchanged.
     */
    public Mono<StartOrResumeResponse> startOrResumeSessionByTopic(
            String topicCode, int count, UUID userId, String userLocale,
            ProgressTagSetId progressTagSetId, String itemType, String answerMode) {
        if (topicCode == null || topicCode.isBlank()) {
            return Mono.error(new SamskrtamException("TOPIC_EMPTY", "Topic code must not be empty"));
        }
        return quizComposeService.compose(userId, topicCode, progressTagSetId,
                        itemType, answerMode, count)
                .map(QuizSessionService::toStartOrResumeResponse);
    }

    private static StartOrResumeResponse toStartOrResumeResponse(ComposeQuizResponse compose) {
        return StartOrResumeResponse.builder()
                .sessionId(compose.getSessionId())
                .lessonId(null)
                .lessonType(null)
                .questions(compose.getQuestions())
                .totalQuestions(compose.getTotalQuestions())
                .answeredQuestions(compose.getAnsweredQuestions())
                .score(compose.getScore())
                .currentQuestionIndex(compose.getCurrentQuestionIndex())
                .currentQuestionNumber(compose.getCurrentQuestionNumber())
                .build();
    }

        private Mono<StartOrResumeResponse> startOrResumeWithFilterScope(
            UUID lessonId, UUID userId, String userLocale,
            FilterScope filterScope, String filterCaseTypes,
            String filterNumberTypes, String filterCombinations,
            String filterVowelTypes, String filterGenders) {
        String scope = filterScope.name();
        String canonicalCaseTypes = null;
        String canonicalNumberTypes = null;
        String canonicalCombinations = null;
        String canonicalVowelTypes = null;
        String canonicalGenders = null;

                switch (filterScope) {
            case CASE_ONLY -> canonicalCaseTypes = QuizFilterJsonHelper.buildCanonicalJsonArray(
                    QuizFilterJsonHelper.parseCsvToList(filterCaseTypes));
            case NUMBER_ONLY -> canonicalNumberTypes = QuizFilterJsonHelper.buildCanonicalJsonArray(
                    QuizFilterJsonHelper.parseCsvToList(filterNumberTypes));
            case CASE_NUMBER_GENDER -> canonicalCombinations = QuizFilterJsonHelper.buildCanonicalCombinationsJson(
                    QuizFilterJsonHelper.parseCombinations(filterCombinations));
                        case ALL_STEMS -> {
                canonicalVowelTypes = QuizFilterJsonHelper.buildCanonicalJsonArray(
                        QuizFilterJsonHelper.parseCsvToList(filterVowelTypes));
                canonicalNumberTypes = QuizFilterJsonHelper.buildCanonicalJsonArray(
                        QuizFilterJsonHelper.parseCsvToList(filterNumberTypes));
                canonicalGenders = QuizFilterJsonHelper.buildCanonicalJsonArray(
                        QuizFilterJsonHelper.parseCsvToList(filterGenders));
                canonicalCaseTypes = QuizFilterJsonHelper.buildCanonicalJsonArray(
                        QuizFilterJsonHelper.parseCsvToList(filterCaseTypes));
            }
        }

        final String finalCaseTypes = canonicalCaseTypes;
        final String finalNumberTypes = canonicalNumberTypes;
        final String finalCombinations = canonicalCombinations;
        final String finalVowelTypes = canonicalVowelTypes;
        final String finalGenders = canonicalGenders;

                if (filterScope == FilterScope.ALL_STEMS) {
            return quizSessionRepository
                    .findInProgressByAllStemsFilter(userId, lessonId, scope,
                            finalVowelTypes, finalGenders, finalNumberTypes, finalCaseTypes)
                    .flatMap(session -> sessionOperationsService.resume(session, userLocale))
                    .switchIfEmpty(Mono.defer(() -> sessionCreationService.createFilteredSession(
                            lessonId, userId, userLocale,
                            filterScope, finalCaseTypes, finalNumberTypes, null,
                            finalVowelTypes, finalGenders)));
        }

        return quizSessionRepository
                .findInProgressByFilter(userId, lessonId, scope,
                        finalCaseTypes, finalNumberTypes, finalCombinations)
                .flatMap(session -> sessionOperationsService.resume(session, userLocale))
                                .switchIfEmpty(Mono.defer(() -> sessionCreationService.createFilteredSession(lessonId, userId, userLocale,
                        filterScope, finalCaseTypes, finalNumberTypes, finalCombinations, null, null)));
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
                                .switchIfEmpty(Mono.defer(() -> sessionCreationService.createStatusFilteredSession(
                        lessonId, userId, userLocale, statusFilter)));
    }

    public Mono<StartOrResumeResponse> resumeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> isComposed(session)
                        ? composedSessionService.resume(session, userLocale)
                        : sessionOperationsService.resume(session, userLocale));
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> isComposed(session)
                        ? composedSessionService.submitAnswer(session, userId, request, userLocale)
                        : sessionOperationsService.submitAnswer(session, userId, request, userLocale));
    }

    public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> isComposed(session)
                        ? composedSessionService.complete(session)
                        : sessionOperationsService.completeSession(session));
    }

    public Mono<StartOrResumeResponse> retakeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> isComposed(session)
                        ? composedSessionService.retake(session, userLocale)
                        : sessionOperationsService.retakeSession(session, userLocale));
    }

    public Mono<StartOrResumeResponse> startNewQuizFromExistingSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                                .flatMap(session -> sessionOperationsService.startNewQuizFromExistingSession(session)
                        .flatMap(completedSession ->
                                sessionCreationService.createNewSession(completedSession.getLessonId(), userId, userLocale)));
    }
}
