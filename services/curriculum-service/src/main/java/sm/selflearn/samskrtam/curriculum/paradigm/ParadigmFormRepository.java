package sm.selflearn.samskrtam.curriculum.paradigm;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParadigmFormRepository extends JpaRepository<ParadigmForm, ParadigmFormId> {

    List<ParadigmForm> findByDeclensionStemId(UUID declensionStemId);
}