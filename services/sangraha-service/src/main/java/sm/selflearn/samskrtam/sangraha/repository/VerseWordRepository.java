package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;

import java.util.List;
import java.util.UUID;

@Repository
public interface VerseWordRepository extends JpaRepository<VerseWord, UUID> {

    List<VerseWord> findAllByVerseIdOrderByPositionAsc(UUID verseId);

    void deleteAllByVerseId(UUID verseId);

    @Modifying
    @Query("UPDATE VerseWord vw SET vw.vocabularyWordId = :vocabularyWordId, vw.vocabSyncStatus = :status WHERE vw.id = :verseWordId")
    void updateVocabularySync(@Param("verseWordId") UUID verseWordId,
                              @Param("vocabularyWordId") UUID vocabularyWordId,
                              @Param("status") String status);
}