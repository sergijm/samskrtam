package sm.selflearn.samskrtam.monierwilliams.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;

import java.util.List;
import java.util.Optional;

@Repository
public interface MwEntryRepository extends JpaRepository<MwEntry, Integer> {

    Optional<MwEntry> findByRecordIdFull(String recordIdFull);

    @Query("SELECT e FROM MwEntry e WHERE e.key1 = :key1")
    List<MwEntry> findByKey1(@Param("key1") String key1);

    List<MwEntry> findByKey1Normalized(String key1);

    @Query(value = "SELECT * FROM cologne_mw.entry e WHERE e.key1_iast_plain = lingua.normalize_lemma(?1)",
            nativeQuery = true)
    List<MwEntry> findByLemmaIast(String query);
}
