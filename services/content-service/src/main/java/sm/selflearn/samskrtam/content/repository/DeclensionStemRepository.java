package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.DeclensionStem;
import sm.selflearn.samskrtam.content.model.VowelType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeclensionStemRepository extends JpaRepository<DeclensionStem, UUID> {
    Optional<DeclensionStem> findByStemNameIast(String stemNameIast);
    List<DeclensionStem> findByVowelType(VowelType vowelType);
}

