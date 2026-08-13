package sm.selflearn.samskrtam.sangraha.repository;

import sm.selflearn.samskrtam.sangraha.model.LemmaStatistics;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LemmaStatisticsRepository extends JpaRepository<LemmaStatistics, UUID> {

    Optional<LemmaStatistics> findByLemmaIdAndGender(UUID lemmaId, String gender);

    List<LemmaStatistics> findByLemmaIdIn(Collection<UUID> lemmaIds);

    /**
     * Все уникальные пары (лемма, род) из {@code lemma_statistics}, отсортированные
     * по частоте вхождения по убыванию (keyset-курсор по (occurrenceCount, id)).
     * Без фильтра по классификации — строки без классификации тоже экспортируются.
     */
    @Query("""
            SELECT s FROM LemmaStatistics s
            WHERE (:cursorCount IS NULL
                OR s.occurrenceCount < :cursorCount
                OR (s.occurrenceCount = :cursorCount AND s.id > :cursorId))
            ORDER BY s.occurrenceCount DESC, s.id ASC
            """)
    List<LemmaStatistics> findForExport(
            @Param("cursorCount") Integer cursorCount,
            @Param("cursorId") UUID cursorId,
            Pageable pageable);

    /**
     * Пересчёт статистики для переданных лемм через нативную функцию
     * {@code sangraha.compute_lemma_statistics} (оконные функции): каждая строка
     * (lemma_id, gender) добавляется или обновляется (upsert по UNIQUE
     * (lemma_id, gender)). При {@code lemmaIds == null} — пересчёт по всем леммам.
     */
    @Modifying
    @Query(value = """
            INSERT INTO sangraha.lemma_statistics (id, lemma_id, gender, occurrence_count, dominant_pos_code, updated_at)
            SELECT gen_random_uuid(), s.lemma_id, s.gender, s.occurrence_count, s.dominant_pos_code, now()
            FROM sangraha.compute_lemma_statistics(:lemmaIds) s
            ON CONFLICT (lemma_id, gender) DO UPDATE SET
                occurrence_count = EXCLUDED.occurrence_count,
                dominant_pos_code = EXCLUDED.dominant_pos_code,
                updated_at = now()
            """, nativeQuery = true)
    int refreshStatistics(@Param("lemmaIds") UUID[] lemmaIds);
}