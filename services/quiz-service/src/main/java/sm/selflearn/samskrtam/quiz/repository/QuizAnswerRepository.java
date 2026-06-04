package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;

import java.util.UUID;

@Repository
public interface QuizAnswerRepository extends ReactiveCrudRepository<QuizAnswer, UUID> {
    Flux<QuizAnswer> findBySessionId(UUID sessionId);
    Mono<Boolean> existsBySessionIdAndQuestionId(UUID sessionId, UUID questionId);
}
