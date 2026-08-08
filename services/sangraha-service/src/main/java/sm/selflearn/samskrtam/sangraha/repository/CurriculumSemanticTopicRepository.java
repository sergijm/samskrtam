package sm.selflearn.samskrtam.sangraha.repository;

import sm.selflearn.samskrtam.sangraha.model.CurriculumSemanticTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurriculumSemanticTopicRepository extends JpaRepository<CurriculumSemanticTopic, String> {

    List<CurriculumSemanticTopic> findByParentIsNullOrderByCodeAsc();

    List<CurriculumSemanticTopic> findByParentParentIsNullOrderByCodeAsc();
}