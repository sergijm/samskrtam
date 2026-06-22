package sm.selflearn.samskrtam.monierwilliams.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.monierwilliams.entity.MwSanskritWord;

import java.util.List;
import java.util.Optional;

@Repository
public interface MwSanskritWordRepository extends JpaRepository<MwSanskritWord, Integer> {
    List<MwSanskritWord> findByEntryIdOrderByPositionOrder(Integer entryId);
    List<MwSanskritWord> findBySlp1SpellingStartingWithIgnoreCase(String prefix);
    List<MwSanskritWord> findBySlp1Spelling(String slp1Spelling);
    List<MwSanskritWord> findBySlp1SpellingIgnoreCase(String slp1Spelling);
    List<MwSanskritWord> findBySlp1NormalizedIgnoreCase(String slp1Spelling);
    List<MwSanskritWord> findBySlp1Normalized(String slp1Spelling);
    Optional<MwSanskritWord> findByEntryIdAndIsPrimaryHeadwordTrue(Integer entryId);

}
