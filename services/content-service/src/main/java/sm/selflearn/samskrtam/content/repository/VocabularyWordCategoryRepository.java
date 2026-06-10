package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.VocabularyWordCategory;
import sm.selflearn.samskrtam.content.model.VocabularyWordCategoryId;

import java.util.List;
import java.util.UUID;

@Repository
public interface VocabularyWordCategoryRepository extends JpaRepository<VocabularyWordCategory, VocabularyWordCategoryId> {
    List<VocabularyWordCategory> findByCategoryId(UUID categoryId);
}
