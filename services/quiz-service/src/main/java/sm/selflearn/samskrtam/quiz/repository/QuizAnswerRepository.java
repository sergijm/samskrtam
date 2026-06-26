package sm.selflearn.samskrtam.quiz.repository;

import io.swagger.v3.oas.annotations.media.Schema;
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
    Mono<Boolean> existsBySessionIdAndQuestionId(UUID sessionId, UUID questionId); // Changed from SessionQuestionId

    // New method to delete all answers for a given session
    Mono<Void> deleteBySessionId(UUID sessionId);

    // Removed ORDER BY :#{#pageable.sort} to avoid null binding issue
    @Query("SELECT * FROM quiz.quiz_answers WHERE session_id = :sessionId LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<QuizAnswer> findSessionAnswers(UUID sessionId, Pageable pageable);

        @Query("SELECT COUNT(*) FROM quiz.quiz_answers WHERE session_id = :sessionId")
    Mono<Long> countBySessionId(UUID sessionId);

    /**
     * Находит все ответы пользователя по конкретному слову в рамках конкретного квиза.
     * JOIN через session_questions, которая хранит vocabulary_word_id.
     */
    @Query("""
            SELECT qa.*
            FROM quiz.quiz_answers qa
            JOIN quiz.session_questions sq ON sq.question_id = qa.question_id
            JOIN quiz.quiz_session qs ON qs.id = qa.session_id
            WHERE sq.vocabulary_word_id = :wordId
              AND qs.user_id = :userId
            ORDER BY qa.answered_at DESC
            """)
    Flux<QuizAnswer> findByWordIdAndUserId(UUID wordId, UUID userId);

    @Query("""
            SELECT COUNT(*)
            FROM quiz.quiz_answers qa
            JOIN quiz.session_questions sq ON sq.question_id = qa.question_id
            JOIN quiz.quiz_session qs ON qs.id = qa.session_id
            WHERE sq.vocabulary_word_id = :wordId
              AND qs.user_id = :userId
            """)
    Mono<Long> countByWordIdAndUserId(UUID wordId, UUID userId);


}
