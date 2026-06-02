package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.Case;
import sm.selflearn.samskrtam.content.model.DeclensionForm;
import sm.selflearn.samskrtam.content.model.DeclensionFormId;
import sm.selflearn.samskrtam.content.model.Number;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeclensionFormRepository extends JpaRepository<DeclensionForm, DeclensionFormId> {
    List<DeclensionForm> findByDeclensionStemId(UUID declensionStemId);
    Optional<DeclensionForm> findByDeclensionStemIdAndCaseTypeAndNumberType(UUID declensionStemId, Case caseType, Number numberType);
}
