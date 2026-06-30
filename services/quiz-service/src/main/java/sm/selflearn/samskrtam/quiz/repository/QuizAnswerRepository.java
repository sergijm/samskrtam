package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.WordScoreDto;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;

import java.util.UUID;

@Repository
public interface QuizAnswerRepository extends ReactiveCrudRepository<QuizAnswer, UUID> {
    Flux<QuizAnswer> findBySessionId(UUID sessionId);

    Mono<Boolean> existsBySessionIdAndQuestionId(UUID sessionId, UUID questionId);
    Mono<Void> deleteBySessionId(UUID sessionId);

    @Query("SELECT * FROM quiz.quiz_answers WHERE session_id = :sessionId LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<QuizAnswer> findSessionAnswers(UUID sessionId, Pageable pageable);

        @Query("SELECT COUNT(*) FROM quiz.quiz_answers WHERE session_id = :sessionId")
    Mono<Long> countBySessionId(UUID sessionId);

    // ... existing code for findByWordIdAndUserIdAndLessonId ...
    @Query("""
            SELECT qa.*
            FROM quiz.quiz_answers qa
            JOIN quiz.session_questions sq ON sq.question_id = qa.question_id
            JOIN quiz.quiz_session qs ON qs.id = qa.session_id
            WHERE sq.vocabulary_word_id = :wordId
              AND qs.user_id = :userId
              AND qs.lesson_id = :lessonId
            ORDER BY qa.answered_at DESC
            """)
    Flux<QuizAnswer> findByWordIdAndUserIdAndLessonId(UUID wordId, UUID userId, UUID lessonId);

        @Query("""
            SELECT COUNT(*)
            FROM quiz.quiz_answers qa
            JOIN quiz.session_questions sq ON sq.question_id = qa.question_id
            JOIN quiz.quiz_session qs ON qs.id = qa.session_id
            WHERE sq.vocabulary_word_id = :wordId
              AND qs.user_id = :userId
              AND qs.lesson_id = :lessonId
            """)
    Mono<Long> countByWordIdAndUserIdAndLessonId(UUID wordId, UUID userId, UUID lessonId);

    @Query("""
            SELECT
              CAST(COUNT(*) AS BIGINT) AS totalAttempts,
              CAST(SUM(CASE WHEN qa.is_correct THEN 1 ELSE 0 END) AS BIGINT) AS correctAnswers,
              MAX(qa.answered_at) AS lastSeenAt
            FROM quiz.quiz_answers qa
            JOIN quiz.quiz_session qs ON qa.session_id = qs.id
            JOIN quiz.session_questions sq ON qa.question_id = sq.id
            WHERE qs.user_id = :userId
              AND qs.lesson_id = :lessonId
              AND sq.vocabulary_word_id = :wordId
            """)
    Mono<WordScoreDto> calculateWordScore(UUID userId, UUID lessonId, UUID wordId);

    @Query("""
            SELECT qa.id, qa.is_correct AS isCorrect, qa.answered_at AS answeredAt,
                   sq.correct_form_iast AS correctFormIast, qa.selected_answer AS selectedAnswer
            FROM quiz.quiz_answers qa
            JOIN quiz.session_questions sq ON qa.question_id = sq.id
            JOIN quiz.quiz_session qs ON qa.session_id = qs.id
            WHERE sq.target_case = :caseType
              AND sq.target_number = :numberType
              AND qs.user_id = :userId
              AND qs.lesson_id = :lessonId
            ORDER BY qa.answered_at DESC
            LIMIT :size OFFSET :offset
            """)
    Flux<QuizAnswerHistoryProjection> findGrammarHistory(
            String caseType, String numberType, UUID userId, UUID lessonId, int size, long offset);

    @Query("""
            SELECT COUNT(*)
            FROM quiz.quiz_answers qa
            JOIN quiz.session_questions sq ON qa.question_id = sq.id
            JOIN quiz.quiz_session qs ON qa.session_id = qs.id
            WHERE sq.target_case = :caseType
              AND sq.target_number = :numberType
              AND qs.user_id = :userId
              AND qs.lesson_id = :lessonId
            """)
    Mono<Long> countGrammarHistory(
            String caseType, String numberType, UUID userId, UUID lessonId);
}

