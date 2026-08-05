package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.WordForm;

import java.util.List;
import java.util.UUID;

@Repository
public interface WordFormRepository extends JpaRepository<WordForm, UUID> {
    List<WordForm> findByLexemeId(UUID lexemeId);

    List<WordForm> findByFormIastIgnoreCase(String formIast);

    long countByLexemeId(UUID lexemeId);
}