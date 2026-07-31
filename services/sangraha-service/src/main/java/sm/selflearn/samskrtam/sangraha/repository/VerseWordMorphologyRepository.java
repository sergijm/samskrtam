package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.VerseWordMorphology;

import java.util.UUID;

@Repository
public interface VerseWordMorphologyRepository extends JpaRepository<VerseWordMorphology, UUID> {
    void deleteByVerseWordId(UUID verseWordId);
}
