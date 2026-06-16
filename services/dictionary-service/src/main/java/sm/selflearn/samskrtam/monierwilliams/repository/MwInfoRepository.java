package sm.selflearn.samskrtam.monierwilliams.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.monierwilliams.entity.MwInfo;

import java.util.List;

@Repository
public interface MwInfoRepository extends JpaRepository<MwInfo, Integer> {
    List<MwInfo> findByEntryId(Integer entryId);
}
