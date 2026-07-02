package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.VocabularyWord;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VocabularyWordRepository extends JpaRepository<VocabularyWord, UUID> {

    Optional<VocabularyWord> findByWordIastAndStem(String wordIast, String stem);
}

