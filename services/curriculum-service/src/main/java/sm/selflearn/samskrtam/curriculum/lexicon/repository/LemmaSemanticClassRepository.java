package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaSemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaSemanticClassId;

import java.util.List;
import java.util.UUID;

@Repository
public interface LemmaSemanticClassRepository extends JpaRepository<LemmaSemanticClass, LemmaSemanticClassId> {

    List<LemmaSemanticClass> findById_LemmaTranslationId(UUID lemmaTranslationId);

    List<LemmaSemanticClass> findById_SemanticClassId(UUID semanticClassId);

    void deleteById_LemmaTranslationId(UUID lemmaTranslationId);
}
