package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.WorksClass;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorksClassRepository extends JpaRepository<WorksClass, UUID> {

    List<WorksClass> findAllByOrderByClassificationAscSortOrderAsc();

    List<WorksClass> findByParentId(UUID parentId);

    List<WorksClass> findByParentIdIsNull();
}