package sm.selflearn.samskrtam.sangraha.repository;

import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LemmaRepository extends JpaRepository<Lemma, UUID> {

    Optional<Lemma> findByLemmaSlp1AndGender(String lemmaSlp1, String gender);

    List<Lemma> findByLemmaSlp1(String lemmaSlp1);

    /**
     * Кандидаты на классификацию: леммы БЕЗ строки LemmaClassification по схеме
     * с {@code status != REJECTED} — ранее отклонённые не подставляются повторно
     * (lemma-classification.md §3 шаг 1). Сортировка по frequencyRank ASC —
     * сначала самые частотные.
     */
    @Query("""
            SELECT l FROM Lemma l
            WHERE NOT EXISTS (
                SELECT 1 FROM LemmaClassification lc
                WHERE lc.lemma = l AND lc.schemeCode = :schemeCode AND lc.status <> :rejected
            )
            ORDER BY l.frequencyRank ASC, l.lemmaSlp1 ASC
            """)
    List<Lemma> findCandidatesForClassification(
            @Param("schemeCode") String schemeCode,
            @Param("rejected") ClassificationStatus rejected,
            Pageable pageable);
}