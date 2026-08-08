package sm.selflearn.samskrtam.sangraha.repository;

import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.model.LemmaClassification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LemmaClassificationRepository extends JpaRepository<LemmaClassification, UUID> {

    Optional<LemmaClassification> findByLemmaIdAndSchemeCode(UUID lemmaId, String schemeCode);

    /**
     * Список на ревью: строки схемы со статусом, отсортированные по
     * frequencyRank леммы (частотные приоритетнее, §4). Курсор — lemmaId >
     * lastLemmaId.
     */
    @Query("""
            SELECT lc FROM LemmaClassification lc
            JOIN lc.lemma l
            WHERE lc.schemeCode = :schemeCode AND lc.status = :status
              AND (:cursor IS NULL OR l.id > :cursor)
            ORDER BY l.frequencyRank ASC, l.id ASC
            """)
    List<LemmaClassification> findForReview(
            @Param("schemeCode") String schemeCode,
            @Param("status") ClassificationStatus status,
            @Param("cursor") UUID cursor,
            Pageable pageable);

    List<LemmaClassification> findBySchemeCodeAndStatus(String schemeCode, ClassificationStatus status);
}