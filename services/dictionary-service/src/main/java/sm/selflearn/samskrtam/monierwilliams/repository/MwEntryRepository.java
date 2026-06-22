package sm.selflearn.samskrtam.monierwilliams.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;
import sm.selflearn.samskrtam.monierwilliams.model.SanskritWordSearchResult;

import java.util.List;
import java.util.Optional;

@Repository
public interface MwEntryRepository extends JpaRepository<MwEntry, Integer> {

    Optional<MwEntry> findByRecordIdFull(String recordIdFull);

    @Query("SELECT e FROM MwEntry e WHERE e.key1 = :key1")
    List<MwEntry> findByKey1(@Param("key1") String key1);

    List<MwEntry> findByKey1Normalized(String key1);

    @Query(value = "SELECT " +
            "e.key1 as slp1_spelling, " +
            "e.key1_normalized as slp1_normalized, " +
            "e.key1_iast as iast_spelling, " +
            "GREATEST (similarity(e.key1_normalized, ?1), similarity(e.key1_iast_plain, ?1)) as similarity " +
            "FROM cologne_mw.entry e " +
            "WHERE similarity(e.key1_normalized, ?1) > 0.5  or similarity(e.key1_iast_plain, ?1) > 0.5 "
            , nativeQuery = true)
    List<SanskritWordSearchResult> findWordsByKey1NormalizedSimilarity(String normalizedQuery);
}
