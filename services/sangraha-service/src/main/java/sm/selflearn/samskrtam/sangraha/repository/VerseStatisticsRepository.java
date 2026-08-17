package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.VerseStatistics;

import java.util.UUID;

/**
 * Предвычисленная статистика стиха — длина в словах и грамматический состав
 * ({@code verse_statistics}, см. {@link sm.selflearn.samskrtam.sangraha.model.VerseStatistics}).
 */
@Repository
public interface VerseStatisticsRepository extends JpaRepository<VerseStatistics, UUID> {

    /**
     * Удаление возвращает 0 при отсутствии слов в verse_words
     * (LEFT JOIN vs COUNT) — актуально и для обновления:
     * пересчёт word_count и grammar_info всех стихов (sangraha-service.md §9).
     * Строки soft-удалённых стихов (deleted_at) не считаются. Возвращает число
     * изменённых строк.
     */
    @Modifying
    @Query(value = """
            INSERT INTO sangraha.verse_statistics (verse_id, word_count, grammar_info, updated_at)
            SELECT
                v.id,
                COUNT(w.id)::int,
                jsonb_build_object(
                    'pos',        COALESCE(jsonb_agg(DISTINCT w.pos) FILTER (WHERE w.pos IS NOT NULL), '[]'::jsonb),
                    'formType',   COALESCE(jsonb_agg(DISTINCT w.form_type) FILTER (WHERE w.form_type IS NOT NULL), '[]'::jsonb),
                    'numberType', COALESCE(jsonb_agg(DISTINCT m.number_type) FILTER (WHERE m.number_type IS NOT NULL), '[]'::jsonb),
                    'caseType',   COALESCE(jsonb_agg(DISTINCT m.case_type) FILTER (WHERE m.case_type IS NOT NULL), '[]'::jsonb),
                    'gender',     COALESCE(jsonb_agg(DISTINCT m.gender) FILTER (WHERE m.gender IS NOT NULL), '[]'::jsonb)
                ),
                now()
            FROM sangraha.verses v
            LEFT JOIN sangraha.verse_words w ON w.verse_id = v.id
            LEFT JOIN sangraha.verse_word_morphology m ON m.verse_word_id = w.id
            WHERE v.deleted_at IS NULL
            GROUP BY v.id
            ON CONFLICT (verse_id) DO UPDATE SET
                word_count = EXCLUDED.word_count,
                grammar_info = EXCLUDED.grammar_info,
                updated_at = now()
            """, nativeQuery = true)
    int refreshStatistics();
}