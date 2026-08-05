package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserCollectionItem;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserCollectionItemId;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserCollectionItemRepository extends JpaRepository<UserCollectionItem, UserCollectionItemId> {
    List<UserCollectionItem> findByIdCollectionId(UUID collectionId);

    boolean existsByIdCollectionIdAndIdLexemeId(UUID collectionId, UUID lexemeId);

    long countByIdCollectionId(UUID collectionId);
}