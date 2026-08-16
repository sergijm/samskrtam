package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LexemeRepository extends JpaRepository<Lexeme, UUID> {
    Optional<Lexeme> findByLemmaSlp1(String lemmaSlp1);

    Optional<Lexeme> findByLemmaSlp1AndGender(String lemmaSlp1, LexemeGender gender);

    boolean existsByLemmaSlp1AndGender(String lemmaSlp1, LexemeGender gender);

    /** Все значения написания, по возрастанию meaningNumber — для поиска по идентичности значения. */
    List<Lexeme> findByLemmaSlp1AndGenderOrderByMeaningNumberAsc(String lemmaSlp1, LexemeGender gender);

    /**
     * Максимальный {@code meaningNumber} написания (0, если строк ещё нет).
     * Новое значение инкрементальной пачки получает max+1 (lexicon-content-pipeline.md §7).
     */
    @Query("select coalesce(max(l.meaningNumber), 0) from Lexeme l where l.lemmaSlp1 = :lemmaSlp1")
    int findMaxMeaningNumber(@Param("lemmaSlp1") String lemmaSlp1);

    /**
     * Lexeme ids tagged with any of the given semantic classes. Used by the pool
     * resolver to translate topic ids into a lexeme pool in one query.
     */
    @Query("select distinct l.id from Lexeme l join l.semanticClasses s where s.id in :semanticClassIds")
    List<UUID> findLexemeIdsBySemanticClassIds(@Param("semanticClassIds") Collection<UUID> semanticClassIds);

    List<Lexeme> findByLemmaIastStartingWith(String prefix);

    /** Lexemes whose lemma IAST is exactly one of the given values (e.g. suppletive pronouns). */
    List<Lexeme> findByLemmaIastIn(Collection<String> lemmaIasts);

    List<Lexeme> findByMorphologyClasses_Code(String morphologyClassCode);

    long countByMorphologyClasses_Code(String morphologyClassCode);

    List<Lexeme> findByPartsOfSpeech_CodeIn(Collection<String> posCodes);

    List<Lexeme> findByMorphologyClasses_CodeIn(Collection<String> morphologyCodes);

    /**
     * Distinct lexemes bound to any of the given morphology classes, with the
     * {@code morphologyClasses} collection eagerly fetched (join fetch). Used by
     * the declension generator to resolve which specific class a lexeme belongs
     * to (a lexeme may be bound to several classes).
     */
    @Query("select distinct l from Lexeme l join fetch l.morphologyClasses mc where mc.code in :codes")
    List<Lexeme> findWithMorphologyByCodeIn(@Param("codes") Collection<String> codes);

    /**
     * Distinct <b>noun</b> lexemes bound to any of the given morphology classes,
     * with {@code morphologyClasses} eagerly fetched.
     */
    @Query("select distinct l from Lexeme l join fetch l.morphologyClasses mc "
         + "join l.partsOfSpeech pos "
         + "where mc.code in :codes and pos.code = 'noun'")
    List<Lexeme> findNounsWithMorphologyByCodeIn(@Param("codes") Collection<String> codes);

    /**
     * Semantic class ids any of the given lexemes are bound to (one row per
     * binding). Used by the dashboard to aggregate per-user mastered counts per
     * semantic class, mirroring the {@code semantic_class_lexeme_counts} view.
     */
    @Query("select lcs.id from Lexeme l join l.semanticClasses lcs where l.id in :lexemeIds")
    List<UUID> findSemanticClassIdsByLexemeIds(@Param("lexemeIds") Collection<UUID> lexemeIds);

    /** Projection: part-of-speech code -> distinct lexeme count. */
    interface PosCount {
        String getCode();

        long getCnt();
    }

    @Query("select pos.code as code, count(distinct l.id) as cnt "
            + "from Lexeme l join l.partsOfSpeech pos group by pos.code")
    List<PosCount> countLexemesByPartOfSpeech();

    @EntityGraph(attributePaths = {"partsOfSpeech", "morphologyClasses", "wordForms"})
    List<Lexeme> findWithDetailsByIdIn(Collection<UUID> ids);

    @Query("select distinct l from Lexeme l "
            + "left join l.semanticClasses sc "
            + "where (:posCode is null or :posCode = '' or exists (select p from l.partsOfSpeech p where p.code = :posCode)) "
            + "and (:noSemanticClass = false or sc is null) "
            + "and (:semanticClassId is null or exists (select c from l.semanticClasses c where c.id = :semanticClassId)) "
            + "order by l.lemmaSlp1")
    Page<Lexeme> search(
            @Param("posCode") String posCode,
            @Param("semanticClassId") UUID semanticClassId,
            @Param("noSemanticClass") boolean noSemantic,
            Pageable pageable);
}