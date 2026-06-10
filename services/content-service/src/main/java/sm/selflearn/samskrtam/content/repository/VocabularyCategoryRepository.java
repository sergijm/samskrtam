package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.VocabularyCategory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VocabularyCategoryRepository extends JpaRepository<VocabularyCategory, UUID> {
    Optional<VocabularyCategory> findByCodeIgnoreCase(String code);

    @Query(value = """
        WITH RECURSIVE category_tree AS (
            SELECT id, parent_id
            FROM content.vocabulary_categories
            WHERE id = :categoryId
            UNION ALL
            SELECT vc.id, vc.parent_id
            FROM content.vocabulary_categories vc
            JOIN category_tree ct ON vc.parent_id = ct.id
        )
        SELECT id FROM category_tree
        """, nativeQuery = true)
    List<UUID> findAllChildrenIds(UUID categoryId);
}
