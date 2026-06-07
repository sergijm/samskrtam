package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.QuizType; // Import QuizType
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.util.UUID;

@Repository
public interface QuizSessionRepository extends ReactiveCrudRepository<QuizSession, UUID> {
    Flux<QuizSession> findByUserIdAndStatus(UUID userId, SessionStatus status);
    Mono<QuizSession> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT * FROM quiz.quiz_sessions " +
           "WHERE user_id = :userId " +
           "AND (:quizType IS NULL OR quiz_type = :quizType) " +
           "AND (:status IS NULL OR status = :status) " +
           "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}") // Добавлены LIMIT и OFFSET
    Flux<QuizSession> findUserSessions(UUID userId, QuizType quizType, SessionStatus status, Pageable pageable);

    @Query("SELECT COUNT(*) FROM quiz.quiz_sessions " +
           "WHERE user_id = :userId " +
           "AND (:quizType IS NULL OR quiz_type = :quizType) " +
           "AND (:status IS NULL OR status = :status)")
    Mono<Long> countUserSessions(UUID userId, QuizType quizType, SessionStatus status);
}
