package sm.selflearn.samskrtam.monierwilliams.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.monierwilliams.entity.MwLiterarySource;

import java.util.List;

@Repository
public interface MwLiterarySourceRepository extends JpaRepository<MwLiterarySource, Integer> {
    List<MwLiterarySource> findByEntryIdOrderByPositionOrder(Integer entryId);
}
