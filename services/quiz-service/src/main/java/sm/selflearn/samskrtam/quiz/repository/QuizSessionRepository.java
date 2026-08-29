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

    @Query("SELECT * FROM quiz.quiz_session " +
            "WHERE user_id = CAST(:userId AS uuid) " +
            "AND (:lessonType IS NULL OR lesson_type = :lessonType) " +
            "AND (:status IS NULL OR status = :status) " +
            "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<QuizSession> findUserSessions(UUID userId, LessonType lessonType, SessionStatus status, Pageable pageable);

    @Query("SELECT COUNT(*) FROM quiz.quiz_session " +
            "WHERE user_id = CAST(:userId AS uuid) " +
            "AND (:lessonType IS NULL OR lesson_type = :lessonType) " +
            "AND (:status IS NULL OR status = :status)")
    Mono<Long> countUserSessions(UUID userId, LessonType lessonType, SessionStatus status);

    Mono<QuizSession> findTopByUserIdAndLessonTypeAndStatusOrderByStartedAtDesc(UUID userId, LessonType lessonType, SessionStatus status);

    @Query("SELECT * FROM quiz.quiz_session " +
            "WHERE user_id = CAST(:userId AS uuid) " +
            "AND lesson_id = CAST(:lessonId AS uuid) " +
            "AND status = 'IN_PROGRESS' " +
            "AND progress_tag_set_id IS NULL " +
            "ORDER BY started_at DESC LIMIT 1")
    Mono<QuizSession> findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(UUID userId, UUID lessonId);

    @Modifying
    @Query("UPDATE quiz.quiz_session SET answered_questions = answered_questions + 1, score = CASE WHEN :isCorrect THEN score + 1 ELSE score END WHERE id = CAST(:sessionId AS uuid)")
    Mono<Void> incrementAnsweredQuestionsAndScore(UUID sessionId, boolean isCorrect);

    @Query("SELECT * FROM quiz.quiz_session " +
            "WHERE user_id = CAST(:userId AS uuid) " +
            "AND lesson_id = CAST(:quizId AS uuid) " +
            "AND (:status IS NULL OR status = :status) " +
            "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<QuizSession> findUserSessionsByQuizId(UUID userId, UUID quizId, SessionStatus status, Pageable pageable);

    @Query("SELECT COUNT(*) FROM quiz.quiz_session " +
            "WHERE user_id = CAST(:userId AS uuid) " +
            "AND lesson_id = CAST(:quizId AS uuid) " +
            "AND (:status IS NULL OR status = :status)")
    Mono<Long> countUserSessionsByQuizId(UUID userId, UUID quizId, SessionStatus status);
}