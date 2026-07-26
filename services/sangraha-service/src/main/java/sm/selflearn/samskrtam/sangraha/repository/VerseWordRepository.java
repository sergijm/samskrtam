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

    /**
     * Проверяет, есть ли хотя бы одно слово с {@code vocabularyWordId IS NOT NULL}
     * среди всех стихов всех глав заданного произведения.
     * Используется для вычисления {@code vocabularyQuizAvailable} в {@code VerseDetailDto}.
     */
    @Query("""
        SELECT COUNT(vw) > 0 FROM VerseWord vw
        JOIN Verse v ON vw.verseId = v.id AND v.deletedAt IS NULL
        JOIN Chapter ch ON v.chapterId = ch.id AND ch.deletedAt IS NULL
        WHERE ch.workId = :workId AND vw.vocabularyWordId IS NOT NULL
    """)
    boolean existsSyncedWordsByWorkId(@Param("workId") UUID workId);

    /**
     * Количество уникальных слов с {@code vocabularyWordId IS NOT NULL}
     * на уровне произведения (для отображения на фронтенде).
     */
    @Query("""
        SELECT COUNT(DISTINCT vw.vocabularyWordId) FROM VerseWord vw
        JOIN Verse v ON vw.verseId = v.id AND v.deletedAt IS NULL
        JOIN Chapter ch ON v.chapterId = ch.id AND ch.deletedAt IS NULL
        WHERE ch.workId = :workId AND vw.vocabularyWordId IS NOT NULL
    """)
    int countDistinctSyncedWordsByWorkId(@Param("workId") UUID workId);
}