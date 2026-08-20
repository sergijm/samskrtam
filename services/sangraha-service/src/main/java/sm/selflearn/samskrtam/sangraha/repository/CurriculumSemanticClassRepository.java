package sm.selflearn.samskrtam.sangraha.repository;

import sm.selflearn.samskrtam.sangraha.model.CurriculumSemanticClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurriculumSemanticClassRepository extends JpaRepository<CurriculumSemanticClass, String> {

    List<CurriculumSemanticClass> findByParentIsNullOrderByCodeAsc();

    List<CurriculumSemanticClass> findByParentParentIsNullOrderByCodeAsc();
}