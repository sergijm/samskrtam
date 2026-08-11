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
            "WHERE user_id = :userId " +
            "AND (:lessonType IS NULL OR lesson_type = :lessonType) " +
            "AND (:status IS NULL OR status = :status) " +
            "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<QuizSession> findUserSessions(UUID userId, LessonType lessonType, SessionStatus status, Pageable pageable);

    @Query("SELECT COUNT(*) FROM quiz.quiz_session " +
            "WHERE user_id = :userId " +
            "AND (:lessonType IS NULL OR lesson_type = :lessonType) " +
            "AND (:status IS NULL OR status = :status)")
    Mono<Long> countUserSessions(UUID userId, LessonType lessonType, SessionStatus status);

    Mono<QuizSession> findTopByUserIdAndLessonTypeAndStatusOrderByStartedAtDesc(UUID userId, LessonType lessonType, SessionStatus status);

    @Query("SELECT * FROM quiz.quiz_session " +
            "WHERE user_id = :userId " +
            "AND lesson_id = :lessonId " +
            "AND status = 'IN_PROGRESS' " +
            "AND (:status IS NULL OR status = :status) " +
            "AND progress_tag_set_id IS NULL " +
            "ORDER BY started_at DESC LIMIT 1")
    Mono<QuizSession> findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(UUID userId, UUID lessonId, SessionStatus status);

    @Modifying
    @Query("UPDATE quiz.quiz_session SET answered_questions = answered_questions + 1, score = CASE WHEN :isCorrect THEN score + 1 ELSE score END WHERE id = :sessionId")
    Mono<Void> incrementAnsweredQuestionsAndScore(UUID sessionId, boolean isCorrect);

    /**
     * Find an in-progress session matching the given progress-tag set slice.
     * Resume is matched by the equality of progress_tag_set_id (NULL matches sessions
     * without a slice). See quest-engine.md §2.4, quiz-declension.md §3.4.
     *
     * @param progressTagSetId stable ProgressTagSet id, or null for whole-lesson sessions
     */
    @Query("SELECT * FROM quiz.quiz_session " +
            "WHERE user_id = cast( :userId as UUID)" +
            "AND lesson_id = cast( :lessonId as UUID) " +
            "AND (:status IS NULL OR status = :status) " +
            "AND progress_tag_set_id IS NOT DISTINCT FROM CAST(:progressTagSetId AS VARCHAR) " +
            "ORDER BY started_at DESC LIMIT 1")
    Mono<QuizSession> findInProgressByProgressTagSet(UUID userId, UUID lessonId, SessionStatus status, String progressTagSetId);

    /** Paginated sessions filtered by quizId (lessonId). */
    @Query("SELECT * FROM quiz.quiz_session " +
            "WHERE user_id = :userId " +
            "AND lesson_id = :quizId " +
            "AND (:status IS NULL OR status = :status) " +
            "LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<QuizSession> findUserSessionsByQuizId(UUID userId, UUID quizId, SessionStatus status, Pageable pageable);

    @Query("SELECT COUNT(*) FROM quiz.quiz_session " +
            "WHERE user_id = :userId " +
            "AND lesson_id = :quizId " +
            "AND (:status IS NULL OR status = :status)")
    Mono<Long> countUserSessionsByQuizId(UUID userId, UUID quizId, SessionStatus status);
}

