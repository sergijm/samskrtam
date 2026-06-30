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

    Mono<GrammarFormScore> findByUserIdAndLessonIdAndCaseTypeAndNumberType(
            UUID userId, UUID lessonId, String caseType, String numberType);

    Flux<GrammarFormScore> findByUserIdAndLessonId(UUID userId, UUID lessonId);

    @Query("""
            SELECT COUNT(*) FROM quiz.grammar_form_score
            WHERE user_id = :userId AND lesson_id = :lessonId AND score >= :minScore
            """)
    Mono<Long> countLearnedForms(UUID userId, UUID lessonId, int minScore);

    @Modifying
    @Query("""
            INSERT INTO quiz.grammar_form_score
            (id, user_id, lesson_id, case_type, number_type, score, updated_at)
            VALUES (:id, :userId, :lessonId, :caseType, :numberType, :score, NOW())
            ON CONFLICT (user_id, lesson_id, case_type, number_type)
            DO UPDATE SET score = EXCLUDED.score, updated_at = NOW()
            """)
    Mono<Void> upsertScore(UUID id, UUID userId, UUID lessonId,
                           String caseType, String numberType, int score);
}