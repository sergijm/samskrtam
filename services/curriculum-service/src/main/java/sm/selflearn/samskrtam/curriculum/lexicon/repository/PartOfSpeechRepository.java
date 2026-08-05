package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;

import java.util.Optional;

@Repository
public interface PartOfSpeechRepository extends JpaRepository<PartOfSpeech, String> {
    Optional<PartOfSpeech> findByCode(String code);
}