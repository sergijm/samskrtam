package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.WorksWorkClass;

import java.util.List;
import java.util.UUID;

@Repository
public interface WorksWorkClassRepository extends JpaRepository<WorksWorkClass, WorksWorkClass.Key> {

    @Query("SELECT wwc.workId FROM WorksWorkClass wwc WHERE wwc.classId IN :classIds")
    List<UUID> findWorkIdsByClassIdIn(@Param("classIds") List<UUID> classIds);
}