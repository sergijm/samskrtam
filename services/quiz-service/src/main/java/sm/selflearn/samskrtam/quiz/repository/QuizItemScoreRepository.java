package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Reactive repository for {@link QuizItemScore}.
 *
 * <p>Единая таблица для всех itemType (VOCABULARY_WORD, DECLENSION_FORM и т.д.).
 * Ключ прогресса — (user_id, item_type, progress_tag).
 */
@Repository
public interface QuizItemScoreRepository extends ReactiveCrudRepository<QuizItemScore, UUID> {

    /** Найти единственную запись прогресса по составному ключу. */
    Mono<QuizItemScore> findByUserIdAndItemTypeAndProgressTag(
            UUID userId, ItemType itemType, String progressTag);

    /** Найти все записи пользователя для данного itemType. */
    Flux<QuizItemScore> findByUserIdAndItemType(UUID userId, ItemType itemType);

    /** Найти все записи пользователя для нескольких progressTag данного itemType. */
    @Query("""
            SELECT * FROM quiz.quiz_item_score
            WHERE user_id = :userId
              AND item_type = :itemType
              AND progress_tag IN (:progressTags)
            """)
    Flux<QuizItemScore> findByUserIdAndItemTypeAndProgressTagIn(
            UUID userId, ItemType itemType, List<String> progressTags);

    /** Найти просроченные записи (next_review_at <= now) для пользователя и itemType. */
    @Query("""
            SELECT * FROM quiz.quiz_item_score
            WHERE user_id = :userId
              AND item_type = :itemType
              AND next_review_at <= :now
            ORDER BY next_review_at ASC
            """)
    Flux<QuizItemScore> findDueItems(UUID userId, ItemType itemType, Instant now);

    /** Upsert: атомарная вставка или обновление при конфликте по (user_id, item_type, progress_tag). */
    @Modifying
    @Query("""
            INSERT INTO quiz.quiz_item_score
                (id, user_id, item_type, progress_tag, score, stability,
                 last_answered_at, last_mistake_at, consecutive_mistakes,
                 next_review_at, updated_at)
            VALUES
                (:id, :userId, :itemType::text, :progressTag, :score, :stability,
                 :lastAnsweredAt, :lastMistakeAt, :consecutiveMistakes,
                 :nextReviewAt, NOW())
            ON CONFLICT (user_id, item_type, progress_tag)
            DO UPDATE SET
                score = EXCLUDED.score,
                stability = EXCLUDED.stability,
                last_answered_at = EXCLUDED.last_answered_at,
                last_mistake_at = EXCLUDED.last_mistake_at,
                consecutive_mistakes = EXCLUDED.consecutive_mistakes,
                next_review_at = EXCLUDED.next_review_at,
                updated_at = NOW()
            """)
    Mono<Void> upsertScore(
            UUID id, UUID userId, String itemType, String progressTag,
            int score, int stability,
            Instant lastAnsweredAt, Instant lastMistakeAt,
            int consecutiveMistakes, Instant nextReviewAt);

    /** Подсчёт записей с score >= порога (для статистики MASTERED). */
    @Query("""
            SELECT COUNT(*) FROM quiz.quiz_item_score
            WHERE user_id = :userId
              AND item_type = :itemType
              AND score >= :minScore
            """)
    Mono<Long> countLearnedItems(UUID userId, ItemType itemType, int minScore);

    /** Найти записи LEARNING/DIFFICULT (score &lt; masteredLowerThreshold) — для statusFilter=LEARNING. */
    @Query("""
            SELECT * FROM quiz.quiz_item_score
            WHERE user_id = :userId
              AND item_type = :itemType
              AND progress_tag IN (:progressTags)
              AND score < :masteredLowerThreshold
            """)
    Flux<QuizItemScore> findLearningItems(
            UUID userId, ItemType itemType, List<String> progressTags, int masteredLowerThreshold);

    /** Найти записи REVIEW (score &gt;= masteredLowerThreshold AND next_review_at &lt;= now) — для statusFilter=REVIEW. */
    @Query("""
            SELECT * FROM quiz.quiz_item_score
            WHERE user_id = :userId
              AND item_type = :itemType
              AND progress_tag IN (:progressTags)
              AND score >= :masteredLowerThreshold
              AND next_review_at <= :now
            ORDER BY next_review_at ASC
            """)
    Flux<QuizItemScore> findReviewItems(
            UUID userId, ItemType itemType, List<String> progressTags, int masteredLowerThreshold, Instant now);
}