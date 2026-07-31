package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.VerseWordDerivation;

import java.util.UUID;

@Repository
public interface VerseWordDerivationRepository extends JpaRepository<VerseWordDerivation, UUID> {
    void deleteByVerseWordId(UUID verseWordId);
}
