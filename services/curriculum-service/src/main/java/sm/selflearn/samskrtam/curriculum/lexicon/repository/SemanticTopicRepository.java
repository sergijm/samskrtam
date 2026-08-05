package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticTopic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SemanticTopicRepository extends JpaRepository<SemanticTopic, UUID> {
    Optional<SemanticTopic> findByCode(String code);

    List<SemanticTopic> findByParentId(UUID parentId);

    List<SemanticTopic> findByParentIsNull();
}