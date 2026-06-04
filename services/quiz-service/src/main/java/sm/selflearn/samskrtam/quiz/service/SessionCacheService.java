package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.QuestionResponse;
import sm.selflearn.samskrtam.content.dto.SessionDataResponse;
import sm.selflearn.samskrtam.quiz.model.CachedQuestion;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionCache;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionCacheService {

    private final ReactiveRedisTemplate<String, SessionCache> redisTemplate;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final ContentClient contentClient;

    private String key(UUID sessionId) {
        return "quiz:session:" + sessionId;
    }

    public Mono<SessionCache> get(UUID sessionId) {
        return redisTemplate.opsForValue().get(key(sessionId))
                .switchIfEmpty(restoreFromPostgres(sessionId));
    }

    public Mono<Void> put(UUID sessionId, SessionCache cache) {
        return redisTemplate.opsForValue()
                .set(key(sessionId), cache, Duration.ofHours(1)) // Cache for 1 hour
                .then();
    }

    public Mono<Void> evict(UUID sessionId) {
        return redisTemplate.delete(key(sessionId)).then();
    }

    private Mono<SessionCache> restoreFromPostgres(UUID sessionId) {
        return quizSessionRepository.findById(sessionId)
                .flatMap(session ->
                        quizAnswerRepository.findBySessionId(sessionId).collectList()
                                .flatMap(answers ->
                                        contentClient.getSessionData(session.getQuizId())
                                                .map(sessionData -> buildCache(session, answers, sessionData))
                                )
                )
                .flatMap(cache -> put(sessionId, cache).thenReturn(cache));
    }

    private SessionCache buildCache(QuizSession session, List<QuizAnswer> answers, SessionDataResponse sessionData) {
        Set<UUID> answeredQuestionIds = answers.stream()
                .map(QuizAnswer::getQuestionId)
                .collect(Collectors.toSet());

        int score = (int) answers.stream()
                .filter(QuizAnswer::isCorrect)
                .count();

        List<CachedQuestion> cachedQuestions = sessionData.getQuestions().stream()
                .map(this::mapToCachedQuestion)
                .collect(Collectors.toList());

        return SessionCache.builder()
                .sessionId(session.getId())
                .userId(session.getUserId())
                .quizId(session.getQuizId())
                .quizType(session.getQuizType())
                .questions(cachedQuestions)
                .answeredQuestionIds(answeredQuestionIds)
                .score(score)
                .build();
    }

    private CachedQuestion mapToCachedQuestion(QuestionResponse qr) {
        return CachedQuestion.builder()
                .questionId(qr.getId())
                .text(qr.getText())
                .explanationRu(qr.getExplanationRu()) // Changed from getExplanation
                .explanationEn(qr.getExplanationEn()) // Added explanationEn
                .declensionStemId(qr.getDeclensionStemId())
                .targetCase(qr.getTargetCase())
                .targetNumber(qr.getTargetNumber())
                .correctFormIast(qr.getCorrectFormIast())
                .correctFormDevanagari(qr.getCorrectFormDevanagari())
                .build();
    }
}
