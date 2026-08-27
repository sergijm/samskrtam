package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lemma;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, UUID> {

    Optional<Lemma> findByLemmaIast(String lemmaIast);

    List<Lemma> findByLemmaIastIn(Set<String> lemmaIasts);

    void deleteByLemmaIast(String lemmaIast);

    @Query("SELECT DISTINCT l.lemmaIast FROM Lemma l WHERE l.pos = :pos")
    List<String> findDistinctLemmaIastByPos(@Param("pos") String pos);

    @Query(value = """
            SELECT DISTINCT l.lemma_iast
            FROM curriculum.lemma l
            WHERE l.freq_order BETWEEN :min AND :max
            """, nativeQuery = true)
    List<String> findDistinctLemmaIastByFrequencyRankRange(@Param("min") int min, @Param("max") int max);

    @Query(value = """
            SELECT DISTINCT l.lemma_iast
            FROM curriculum.lemma l
            JOIN curriculum.lemma_semantic_class lsc ON lsc.lemma_id = l.id
            WHERE lsc.semantic_class_id IN :ids
            """, nativeQuery = true)
    List<String> findDistinctLemmaIastBySemanticClassIds(@Param("ids") Set<UUID> ids);

    @Query("SELECT l.pos, COUNT(DISTINCT l.lemmaIast) FROM Lemma l WHERE l.pos IS NOT NULL GROUP BY l.pos")
    List<Object[]> countDistinctLemmasByPos();
}