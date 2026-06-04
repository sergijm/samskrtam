package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.util.UUID;

@Repository
public interface QuizSessionRepository extends ReactiveCrudRepository<QuizSession, UUID> {
    Flux<QuizSession> findByUserIdAndStatus(UUID userId, SessionStatus status);
    Mono<QuizSession> findByIdAndUserId(UUID id, UUID userId);
}
