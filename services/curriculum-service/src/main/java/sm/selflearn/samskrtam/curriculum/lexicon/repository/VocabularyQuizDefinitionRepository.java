package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.VocabularyQuizDefinition;
import sm.selflearn.samskrtam.curriculum.lexicon.model.VocabularyQuizKind;

import java.util.List;
import java.util.UUID;

@Repository
public interface VocabularyQuizDefinitionRepository extends JpaRepository<VocabularyQuizDefinition, UUID> {
    List<VocabularyQuizDefinition> findByKind(VocabularyQuizKind kind);

    List<VocabularyQuizDefinition> findByTopicId(UUID topicId);

    List<VocabularyQuizDefinition> findBySourceId(UUID sourceId);
}