package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaTranslation;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface LemmaTranslationRepository extends JpaRepository<LemmaTranslation, java.util.UUID> {

    List<LemmaTranslation> findByLemmaIast(String lemmaIast);

    List<LemmaTranslation> findByLemmaIastAndLanguage(String lemmaIast, String language);

    Optional<LemmaTranslation> findByLemmaIastAndLanguageAndIsMainTrue(String lemmaIast, String language);

    List<LemmaTranslation> findByLanguage(String language);

    List<LemmaTranslation> findByLemmaIastIn(Set<String> lemmaIasts);

    /** Unique lemmas (headword spellings) bound to the given semantic classes. */
    @Query(value = """
            SELECT DISTINCT lt.lemma_iast
            FROM curriculum.lemma_translation lt
            JOIN curriculum.lemma_semantic_class lsc ON lsc.lemma_translation_id = lt.id
            WHERE lsc.semantic_class_id IN :ids
            """, nativeQuery = true)
    List<String> findDistinctLemmaIastBySemanticClassIds(@Param("ids") Set<UUID> ids);

    /** Unique lemmas whose part-of-speech equals the Friš pos code. */
    @Query("SELECT DISTINCT lt.lemmaIast FROM LemmaTranslation lt WHERE lt.pos = :pos")
    List<String> findDistinctLemmaIastByPos(@Param("pos") String pos);

    /** Unique lemmas within a frequency rank window (lingua.lemma_frequency.row_num). */
    @Query(value = """
            SELECT DISTINCT lt.lemma_iast
            FROM curriculum.lemma_translation lt
            JOIN lingua.lemma_frequency lf ON lf.lemma_iast = lt.lemma_iast
            WHERE lf.row_num BETWEEN :min AND :max
            """, nativeQuery = true)
    List<String> findDistinctLemmaIastByFrequencyRankRange(@Param("min") int min, @Param("max") int max);

    void deleteByLemmaIast(String lemmaIast);
}
