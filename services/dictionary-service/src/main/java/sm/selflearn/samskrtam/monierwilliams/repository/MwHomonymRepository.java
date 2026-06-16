package sm.selflearn.samskrtam.monierwilliams.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.monierwilliams.entity.MwHomonym;

import java.util.List;

@Repository
public interface MwHomonymRepository extends JpaRepository<MwHomonym, Integer> {
    List<MwHomonym> findByEntryIdOrderByPositionOrder(Integer entryId);
}
