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
import sm.selflearn.samskrtam.quiz.model.ProgressTagSetId;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;

import java.util.UUID;

/**
 * Сервис управления квиз-сессиями — фасад с публичными API-методами.
 * Создание сессий делегировано в {@link SessionCreationService},
 * операции над сессиями — в {@link SessionOperationsService}.
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

    /** Plain start-or-resume (no filters) — legacy lesson-based branch. */
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