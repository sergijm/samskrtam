package sm.selflearn.samskrtam.curriculum.lexicon.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.CollectionVisibility;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserCollection;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserCollectionRepository extends JpaRepository<UserCollection, UUID> {
    List<UserCollection> findByOwnerId(UUID ownerId);

    List<UserCollection> findByOwnerIdAndVisibility(UUID ownerId, CollectionVisibility visibility);
}