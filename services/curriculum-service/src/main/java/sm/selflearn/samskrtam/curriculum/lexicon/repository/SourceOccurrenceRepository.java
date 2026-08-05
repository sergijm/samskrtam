package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SourceOccurrence;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourceOccurrenceRepository extends JpaRepository<SourceOccurrence, UUID> {
    List<SourceOccurrence> findBySourceId(UUID sourceId);

    List<SourceOccurrence> findByLexemeId(UUID lexemeId);

    List<SourceOccurrence> findBySourceIdAndLocationRef(UUID sourceId, String locationRef);

    long countBySourceId(UUID sourceId);

    long countDistinctByLexemeIdAndSourceId(UUID lexemeId, UUID sourceId);
}