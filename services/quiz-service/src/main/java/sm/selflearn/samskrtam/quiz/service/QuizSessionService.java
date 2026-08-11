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
import sm.selflearn.samskrtam.quiz.dto.QuestComposeRequest;
import sm.selflearn.samskrtam.quiz.dto.QuestSessionTopicDto;
import sm.selflearn.samskrtam.quiz.dto.StartOrResumeResponse;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizSessionService {

    private final QuizSessionRepository quizSessionRepository;
    private final ComposedSessionService composedSessionService;
    private final QuestComposeService questComposeService;

    public Mono<StartOrResumeResponse> startOrResumeSessionByTopic(
            String topicCode, int count, UUID userId, String userLocale) {
        if (topicCode == null || topicCode.isBlank()) {
            return Mono.error(new SamskrtamException("TOPIC_EMPTY", "Topic code must not be empty"));
        }
        QuestComposeRequest request = new QuestComposeRequest(
                List.of(QuestSessionTopicDto.byCount(topicCode.trim(), count)),
                userLocale);
        return questComposeService.compose(userId, request)
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
                .flatMap(session -> composedSessionService.resume(session, userLocale));
    }

    public Mono<AnswerResponse> submitAnswer(UUID sessionId, UUID userId, AnswerRequest request, String userLocale) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> composedSessionService.submitAnswer(session, userId, request, userLocale));
    }

    public Mono<CompleteSessionResponse> completeSession(UUID sessionId, UUID userId) {
        return quizSessionRepository.findById(sessionId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND", "Session not found: " + sessionId)))
                .filter(session -> session.getUserId().equals(userId))
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(composedSessionService::complete);
    }

    public Mono<StartOrResumeResponse> retakeSession(UUID sessionId, UUID userId, String userLocale) {
        return quizSessionRepository.findByIdAndUserId(sessionId, userId)
                .switchIfEmpty(Mono.error(new SamskrtamException("SESSION_NOT_FOUND",
                        "Session not found or does not belong to user: " + sessionId)))
                .flatMap(session -> composedSessionService.retake(session, userLocale));
    }
}