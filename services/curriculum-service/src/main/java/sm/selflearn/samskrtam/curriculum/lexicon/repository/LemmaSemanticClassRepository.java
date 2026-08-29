package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaSemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaSemanticClassId;

import java.util.List;
import java.util.UUID;

@Repository
public interface LemmaSemanticClassRepository extends JpaRepository<LemmaSemanticClass, LemmaSemanticClassId> {

    List<LemmaSemanticClass> findById_LemmaId(UUID lemmaId);

    List<LemmaSemanticClass> findById_SemanticClassId(UUID semanticClassId);

    void deleteById_LemmaId(UUID lemmaId);

    /**
     * Считает количество уникальных лемм в каждом топике, связывая лемму с
     * топиком через цепочку lemma_semantic_class → semantic_class_topic.
     * Топик попадает в выборку, только если у него есть семантический класс,
     * которому привязана хотя бы одна лемма.
     */
    @Query("SELECT t.code, COUNT(DISTINCT lsc.id.lemmaId) " +
            "FROM Topic t " +
            "JOIN t.semanticClasses sc " +
            "JOIN LemmaSemanticClass lsc ON lsc.id.semanticClassId = sc.id " +
            "GROUP BY t.code")
    List<Object[]> countLemmasByTopicCode();
}
