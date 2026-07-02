package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerseAnalysisRepository extends JpaRepository<VerseAnalysis, UUID> {

    Optional<VerseAnalysis> findByVerseId(UUID verseId);

    void deleteByVerseId(UUID verseId);
}