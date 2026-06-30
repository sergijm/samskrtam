package sm.selflearn.samskrtam.quiz.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.GrammarFormScore;

import java.util.UUID;

@Repository
public interface GrammarFormScoreRepository extends ReactiveCrudRepository<GrammarFormScore, UUID> {

    // Для поиска существующей записи при обновлении счёта (с gender)
    Mono<GrammarFormScore> findByUserIdAndLessonIdAndGenderAndCaseTypeAndNumberType(
            UUID userId, UUID lessonId, String gender, String caseType, String numberType);

    // Для поиска всех записей пользователя по уроку (с gender)
    Flux<GrammarFormScore> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    @Query("""
            SELECT COUNT(*) FROM quiz.grammar_form_score
            WHERE user_id = :userId AND lesson_id = :lessonId AND score >= :minScore
            """)
    Mono<Long> countLearnedForms(UUID userId, UUID lessonId, int minScore);

    @Query("""
            SELECT COALESCE(AVG(CAST(score AS FLOAT)), 0.0)
            FROM quiz.grammar_form_score
            WHERE user_id = :userId
              AND lesson_id = :lessonId
              AND gender = :gender
              AND case_type = :caseType
              AND number_type = :numberType
            """)
    Mono<Double> aggregateSuccessRate(UUID userId, UUID lessonId,
                                      String gender, String caseType, String numberType);

    @Modifying
    @Query("""
            INSERT INTO quiz.grammar_form_score
            (id, user_id, lesson_id, gender, case_type, number_type, score, updated_at)
            VALUES (:id, :userId, :lessonId, :gender, :caseType, :numberType, :score, NOW())
            ON CONFLICT (user_id, lesson_id, gender, case_type, number_type)
            DO UPDATE SET score = EXCLUDED.score, updated_at = NOW()
            """)
    Mono<Void> upsertScore(UUID id, UUID userId, UUID lessonId,
                           String gender, String caseType, String numberType, int score);
}
