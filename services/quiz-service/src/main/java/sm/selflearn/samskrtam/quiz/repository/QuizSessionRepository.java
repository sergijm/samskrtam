package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionStatus;

import java.util.UUID;

@Repository
public interface QuizSessionRepository extends ReactiveCrudRepository<QuizSession, UUID> {
    Flux<QuizSession> findByUserIdAndStatus(UUID userId, SessionStatus status);
    Mono<QuizSession> findByIdAndUserId(UUID id, UUID userId);

    @Query("SELECT * FROM quiz.quiz_sessions " +
           "WHERE user_id = :userId " +
           "AND (:lessonType IS NULL OR lesson_type = :lessonType) " +
           "AND (:status IS NULL OR status = :status) " +
           "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}") // Добавлены LIMIT и OFFSET
    Flux<QuizSession> findUserSessions(UUID userId, LessonType lessonType, SessionStatus status, Pageable pageable);

    @Query("SELECT COUNT(*) FROM quiz.quiz_sessions " +
           "WHERE user_id = :userId " +
           "AND (:lessonType IS NULL OR lesson_type = :lessonType) " +
           "AND (:status IS NULL OR status = :status)")
    Mono<Long> countUserSessions(UUID userId, LessonType lessonType, SessionStatus status);

    // New method to find the latest unfinished quiz session for a user and quiz type
    Mono<QuizSession> findTopByUserIdAndLessonTypeAndStatusOrderByStartedAtDesc(UUID userId, LessonType lessonType, SessionStatus status);

    // New method to find the latest unfinished quiz session for a user and specific quiz ID
    Mono<QuizSession> findTopByUserIdAndQuizIdAndStatusOrderByStartedAtDesc(UUID userId, UUID quizId, SessionStatus status);

    @Modifying
    @Query("UPDATE quiz.quiz_sessions SET answered_questions = answered_questions + 1, score = CASE WHEN :isCorrect THEN score + 1 ELSE score END WHERE id = :sessionId")
    Mono<Void> incrementAnsweredQuestionsAndScore(UUID sessionId, boolean isCorrect);
}
