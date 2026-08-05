package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
}