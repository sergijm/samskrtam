package sm.selflearn.samskrtam.sangraha.repository;

import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LemmaRepository extends JpaRepository<Lemma, UUID> {

    Optional<Lemma> findByLemmaSlp1(String lemmaSlp1);

    /** Словарные строки по набору SLP1 (для оценки new/updated при refresh словаря). */
    List<Lemma> findByLemmaSlp1In(Collection<String> lemmaSlp1s);

    /**
     * Кандидаты на классификацию — леммы, у которых ЕСТЬ хотя бы одна строка
     * статистики с «нереашённым» (lemma, gender) по схеме (lemma_classification
     * отсутствует или {@code status == REJECTED}): ранее отклонённые
     * ADMIN-результаты не подставляются повторно (lemma-classification.md §3
     * шаг 1). Сортировка — в сервисном слое: по сумме occurrenceCount по всем
     * родам леммы, самые частотные первыми (решение 2026-08-09).
     */
    @Query("""
            SELECT l FROM Lemma l
            WHERE EXISTS (
                SELECT 1 FROM LemmaStatistics ls
                WHERE ls.lemma = l
                  AND NOT EXISTS (
                      SELECT 1 FROM LemmaClassification lc
                      WHERE lc.lemma = l AND lc.gender = ls.gender
                        AND lc.schemeCode = :schemeCode AND lc.status <> :rejected
                  )
            )
            """)
    List<Lemma> findCandidatesForClassification(
            @Param("schemeCode") String schemeCode,
            @Param("rejected") ClassificationStatus rejected);
}