package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SemanticClassRepository extends JpaRepository<SemanticClass, UUID> {
    Optional<SemanticClass> findByCode(String code);

    List<SemanticClass> findByParentId(UUID parentId);

    List<SemanticClass> findByParentIsNull();

    /** Projection over the {@code semantic_class_lexeme_counts} view (node + whole subtree). */
    interface SemanticClassLexemeCount {
        String getCode();

        String getNameRu();

        String getNameEn();

        Long getLexemeCount();
    }

    @Query(value = """
            select code,
                   name_ru  as "nameRu",
                   name_en  as "nameEn",
                   lexeme_count as "lexemeCount"
            from curriculum.semantic_class_lexeme_counts
            """, nativeQuery = true)
    List<SemanticClassLexemeCount> findSemanticClassLexemeCounts();
}