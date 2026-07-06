package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;

import java.util.UUID;

@Repository
public interface SessionQuestionRepository extends ReactiveCrudRepository<SessionQuestion, UUID> {

    Flux<SessionQuestion> findBySessionId(UUID sessionId);

    Mono<SessionQuestion> findByQuestionId(UUID questionId);

    Mono<Void> deleteBySessionId(UUID sessionId);
}