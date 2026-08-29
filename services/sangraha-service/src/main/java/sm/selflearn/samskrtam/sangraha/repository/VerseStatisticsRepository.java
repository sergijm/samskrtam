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
     * Логика перенесена в БД-функцию sangraha.refresh_verse_statistics().
     */
    @Modifying
    @Query(value = "SELECT sangraha.refresh_verse_statistics()", nativeQuery = true)
    int refreshStatistics();
}