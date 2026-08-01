package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.DeclensionExampleGroup;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeclensionExampleGroupRepository extends JpaRepository<DeclensionExampleGroup, UUID> {

    Optional<DeclensionExampleGroup> findByVowelTypeAndGenderAndCaseTypeAndNumberType(
            String vowelType, String gender, String caseType, String numberType);
}