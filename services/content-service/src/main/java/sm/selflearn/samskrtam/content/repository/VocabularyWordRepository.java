package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository; // Changed import
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.VocabularyWord;

import java.util.UUID;

@Repository
public interface VocabularyWordRepository extends JpaRepository<VocabularyWord, UUID> { // Changed extends
}
