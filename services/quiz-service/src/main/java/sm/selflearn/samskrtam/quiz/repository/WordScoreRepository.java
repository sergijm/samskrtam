package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.WordScore;

import java.util.UUID;

@Repository
public interface WordScoreRepository extends ReactiveCrudRepository<WordScore, UUID> {

    Mono<WordScore> findByUserIdAndWordIdAndLessonId(UUID userId, UUID wordId, UUID lessonId);

    @Query(value = """
            SELECT COUNT(*) FROM quiz.word_score
            WHERE user_id = :userId AND lesson_id = :lessonId AND score >= :minScore
            """)
    Mono<Long> countLearnedWords(UUID userId, UUID lessonId, int minScore);

    @Modifying
    @Query(value = """
            INSERT INTO quiz.word_score (id, user_id, word_id, lesson_id, score, updated_at)
            VALUES (:id, :userId, :wordId, :lessonId, :score, NOW())
            ON CONFLICT (user_id, word_id, lesson_id)
            DO UPDATE SET score = EXCLUDED.score, updated_at = NOW()
            """)
    Mono<Void> upsertScore(UUID id, UUID userId, UUID wordId, UUID lessonId, int score);
}

