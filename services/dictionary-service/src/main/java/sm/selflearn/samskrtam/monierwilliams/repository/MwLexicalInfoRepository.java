package sm.selflearn.samskrtam.monierwilliams.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.monierwilliams.entity.MwLexicalInfo;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;

import java.util.List;

@Repository
public interface MwLexicalInfoRepository extends JpaRepository<MwLexicalInfo, Integer> {
    List<MwLexicalInfo> findByEntryId(Integer entryId);
}

