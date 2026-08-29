package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VerseRepository extends JpaRepository<Verse, UUID> {

    List<Verse> findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID chapterId);

    Optional<Verse> findByIdAndDeletedAtIsNull(UUID id);

    int countByChapterIdAndDeletedAtIsNull(UUID chapterId);

    /**
     * Число проанализированных (status = ANALYZED) не удалённых стихов произведения
     * (по всем его не удалённым главам). Используется для подписи плитки произведения.
     */
    @Query("""
            SELECT COUNT(v) FROM Verse v
            WHERE v.chapterId IN (
                SELECT c.id FROM Chapter c WHERE c.workId = :workId AND c.deletedAt IS NULL
            )
            AND v.status = :status AND v.deletedAt IS NULL
            """)
    int countAnalyzedByWorkIdAndDeletedAtIsNull(@Param("workId") UUID workId, @Param("status") VerseStatus status);

    /**
     * Общее число не удалённых стихов произведения (по всем его не удалённым главам).
     */
    @Query("""
            SELECT COUNT(v) FROM Verse v
            WHERE v.chapterId IN (
                SELECT c.id FROM Chapter c WHERE c.workId = :workId AND c.deletedAt IS NULL
            )
            AND v.deletedAt IS NULL
            """)
    int countByWorkIdAndDeletedAtIsNull(@Param("workId") UUID workId);

    /**
     * Пакетный поиск стихов по списку ID: только ANALYZED, не удалённые.
     * Не найденные ID просто отсутствуют в результате (sangraha-service.md §9).
     */
    @Query("""
            SELECT v FROM Verse v
            WHERE v.id IN :ids AND v.status = :status AND v.deletedAt IS NULL
            """)
    List<Verse> findAllByIdInAndStatusAndDeletedAtIsNull(
            @Param("ids") Collection<UUID> ids,
            @Param("status") VerseStatus status);

    @Query("""
            SELECT v FROM Verse v
            WHERE v.id IN :ids AND v.deletedAt IS NULL
            """)
    List<Verse> findAllByIdInAndDeletedAtIsNull(
            @Param("ids") Collection<UUID> ids);

    /**
     * Постраничный обход ANALYZED стихов курсором по {@code id > cursor}
     * (lexicon-content-pipeline.md §2 — экспорт VerseWord[] для импорта лексики).
     * Не удалённые, отсортированные по {@code id} ASC.
     */
    @Query("""
            SELECT v FROM Verse v
            WHERE v.status = :status AND v.deletedAt IS NULL
              AND v.chapterId IS NOT NULL
              AND (:cursor IS NULL OR v.id > :cursor)
            ORDER BY v.id
            """)
    List<Verse> findAllByStatusAndDeletedAtIsNullAndIdGreaterThan(
            @Param("status") VerseStatus status,
            @Param("cursor") UUID cursor,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Постраничный обход всех не удалённых стихов курсором по {@code id > cursor}
     * (LemmaRefreshService — леммы агрегируются по всему корпусу без фильтра
     * статуса: часть корпуса загружена внешним скриптом, у таких стихов слова
     * и морфология уже есть при status != ANALYZED).
     */
    @Query("""
            SELECT v FROM Verse v
            WHERE v.deletedAt IS NULL
              AND v.chapterId IS NOT NULL
              AND (:cursor IS NULL OR v.id > :cursor)
            ORDER BY v.id
            """)
    List<Verse> findAllByDeletedAtIsNullAndIdGreaterThan(
            @Param("cursor") UUID cursor,
            org.springframework.data.domain.Pageable pageable);

    /**
     * Standalone-стихи пользователя (страница /analysis): chapter_id = null,
     * не удалённые, не привязанные к произведению/главе, новые сверху.
     */
    @Query("""
            SELECT v FROM Verse v
            WHERE v.chapterId IS NULL
              AND v.ownerId = :ownerId
              AND v.deletedAt IS NULL
            ORDER BY v.createdAt DESC
            """)
List<Verse> findAllByChapterIdIsNullAndOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            @Param("ownerId") UUID ownerId);

    /**
     * Сбрасывает стихи, зависшие в ANALYZING после аварийной остановки,
     * обратно в DRAFT при старте (sangraha-service startup).
     */
    @Modifying
    @Query("UPDATE Verse v SET v.status = :newStatus, v.updatedAt = CURRENT_TIMESTAMP WHERE v.status = :oldStatus")
    int resetStatusByCurrentStatus(@Param("oldStatus") VerseStatus oldStatus, @Param("newStatus") VerseStatus newStatus);
}