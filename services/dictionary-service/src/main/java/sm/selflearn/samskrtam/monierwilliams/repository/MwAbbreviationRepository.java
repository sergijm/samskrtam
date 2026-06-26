package sm.selflearn.samskrtam.monierwilliams.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.monierwilliams.entity.MwAbbreviation;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;

import java.util.List;

@Repository
public interface MwAbbreviationRepository extends JpaRepository<MwAbbreviation, Integer> {
    List<MwAbbreviation> findByEntryIdOrderByPositionOrder(Integer entryId);
}

