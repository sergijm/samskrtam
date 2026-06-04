package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.quiz.model.SessionHistory;

import java.util.UUID;

@Repository
public interface SessionHistoryRepository extends ReactiveCrudRepository<SessionHistory, UUID> {
    Flux<SessionHistory> findByUserId(UUID userId, Pageable pageable);
    Flux<SessionHistory> findByUserIdAndQuizId(UUID userId, UUID quizId, Pageable pageable);
    Flux<SessionHistory> findByUserIdAndQuizType(UUID userId, QuizType quizType, Pageable pageable);
    Flux<SessionHistory> findByUserIdAndQuizIdAndQuizType(UUID userId, UUID quizId, QuizType quizType, Pageable pageable);
    Mono<SessionHistory> findBySessionIdAndUserId(UUID sessionId, UUID userId);
}
