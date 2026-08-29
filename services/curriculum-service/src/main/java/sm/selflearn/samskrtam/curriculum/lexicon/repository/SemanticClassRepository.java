package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SemanticClassRepository extends JpaRepository<SemanticClass, UUID> {
    Optional<SemanticClass> findByCode(String code);

    List<SemanticClass> findByParentId(UUID parentId);

    List<SemanticClass> findByParentIsNull();
}