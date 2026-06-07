package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
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

    @Query("SELECT * FROM quiz.quiz_answers WHERE session_id = :sessionId ORDER BY :#{#pageable.sort} LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<QuizAnswer> findSessionAnswers(UUID sessionId, Pageable pageable);

    @Query("SELECT COUNT(*) FROM quiz.quiz_answers WHERE session_id = :sessionId")
    Mono<Long> countBySessionId(UUID sessionId);
}
