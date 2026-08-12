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
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeStatus;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LexemeRepository extends JpaRepository<Lexeme, UUID> {
    Optional<Lexeme> findByLemmaSlp1(String lemmaSlp1);

    Optional<Lexeme> findByLemmaSlp1AndGender(String lemmaSlp1, LexemeGender gender);

    boolean existsByLemmaSlp1AndGender(String lemmaSlp1, LexemeGender gender);

    List<Lexeme> findByStatus(LexemeStatus status);

    List<Lexeme> findByLemmaIastStartingWith(String prefix);

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

    @EntityGraph(attributePaths = {"partsOfSpeech", "morphologyClasses", "wordForms"})
    List<Lexeme> findWithDetailsByIdIn(Collection<UUID> ids);

    @Query("select distinct l from Lexeme l "
            + "left join l.semanticTopics st "
            + "where (:status is null or l.status = :status) "
            + "and (:posCode is null or :posCode = '' or exists (select p from l.partsOfSpeech p where p.code = :posCode)) "
            + "and (:noSemanticTopic = false or st is null) "
            + "and (:semanticTopicId is null or exists (select t from l.semanticTopics t where t.id = :semanticTopicId)) "
            + "order by l.lemmaSlp1")
    Page<Lexeme> search(
            @Param("status") LexemeStatus status,
            @Param("posCode") String posCode,
            @Param("semanticTopicId") UUID semanticTopicId,
            @Param("noSemanticTopic") boolean noSemantic,
            Pageable pageable);
}