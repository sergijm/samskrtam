package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.WordStatistics;

import java.util.Collection;
import java.util.UUID;

@Repository
public interface WordStatisticsRepository extends ReactiveCrudRepository<WordStatistics, UUID> {
    // Получить статистику по конкретному слову для пользователя
    Mono<WordStatistics> findByUserIdAndVocabularyWordId(UUID userId, UUID vocabularyWordId);

    // Получить статистику по списку слов — используется при загрузке урока
    Flux<WordStatistics> findByUserIdAndVocabularyWordIdIn(UUID userId, Collection<UUID> vocabularyWordIds);

    // UPSERT: создать или обновить агрегат атомарно
// isCorrect передаётся как 1 (true) или 0 (false) для суммирования
    @Modifying
    @Query("""
            INSERT INTO quiz.word_statistics
            (id, user_id, vocabulary_word_id, total_attempts, correct_answers, last_seen_at)
            VALUES
            (gen_random_uuid(), :userId, :vocabularyWordId, 1, :correctIncrement, now())
            ON CONFLICT (user_id, vocabulary_word_id)
            DO UPDATE SET
            total_attempts  = quiz.word_statistics.total_attempts + 1,
            correct_answers = quiz.word_statistics.correct_answers + :correctIncrement,
            last_seen_at    = now()
            """)
    Mono<Void> upsert(UUID userId, UUID vocabularyWordId, int correctIncrement);
}