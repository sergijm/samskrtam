package sm.selflearn.samskrtam.sangraha.repository;

import org.springframework.data.jpa.repository.JpaRepository;
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
              AND (:cursor IS NULL OR v.id > :cursor)
            ORDER BY v.id
            """)
    List<Verse> findAllByStatusAndDeletedAtIsNullAndIdGreaterThan(
            @Param("status") VerseStatus status,
            @Param("cursor") UUID cursor,
            org.springframework.data.domain.Pageable pageable);

}