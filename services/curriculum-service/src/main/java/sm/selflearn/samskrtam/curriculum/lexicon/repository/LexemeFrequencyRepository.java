package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeFrequency;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeFrequencyId;

import java.util.List;
import java.util.UUID;

@Repository
public interface LexemeFrequencyRepository extends JpaRepository<LexemeFrequency, LexemeFrequencyId> {
    List<LexemeFrequency> findByIdLexemeId(UUID lexemeId);

    List<LexemeFrequency> findByIdSourceOrderByRankAsc(String source);

    java.util.Optional<LexemeFrequency> findByIdLexemeIdAndIdSource(UUID lexemeId, String source);

    @Query("select f.id.lexemeId from LexemeFrequency f where f.id.source = :source "
            + "and (:min is null or f.rank >= :min) and (:max is null or f.rank <= :max)")
    List<UUID> findLexemeIdsBySourceAndRankRange(
            @Param("source") String source,
            @Param("min") Integer min,
            @Param("max") Integer max);
}