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

    Mono<QuizSession> findTopByUserIdAndLessonIdAndStatusOrderByStartedAtDesc(UUID userId, UUID lessonId, SessionStatus status);

    @Modifying
    @Query("UPDATE quiz.quiz_session SET answered_questions = answered_questions + 1, score = CASE WHEN :isCorrect THEN score + 1 ELSE score END WHERE id = :sessionId")
    Mono<Void> incrementAnsweredQuestionsAndScore(UUID sessionId, boolean isCorrect);

    /**
     * Find an in-progress session matching the given filter criteria.
     * For CASE_ONLY scope, pass null for filterNumberType and filterGender.
     * For CASE_NUMBER_GENDER scope, pass concrete values.
     */
    @Query("SELECT * FROM quiz.quiz_session " +
            "WHERE user_id = :userId " +
            "AND lesson_id = :lessonId " +
            "AND status = 'IN_PROGRESS' " +
            "AND filter_scope = CAST(:filterScope AS VARCHAR) " +
            "AND filter_case_type = CAST(:filterCaseType AS VARCHAR) " +
            "AND (:filterNumberType IS NULL OR filter_number_type = CAST(:filterNumberType AS VARCHAR)) " +
            "AND (:filterGender IS NULL OR filter_gender = CAST(:filterGender AS VARCHAR)) " +
            "ORDER BY started_at DESC LIMIT 1")
    Mono<QuizSession> findInProgressByFilter(UUID userId, UUID lessonId, String filterScope, String filterCaseType, String filterNumberType, String filterGender);
}

