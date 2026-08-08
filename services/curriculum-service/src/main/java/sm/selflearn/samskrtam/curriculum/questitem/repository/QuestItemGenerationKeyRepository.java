package sm.selflearn.samskrtam.curriculum.questitem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItemGenerationKey;

import java.util.UUID;

@Repository
public interface QuestItemGenerationKeyRepository extends JpaRepository<QuestItemGenerationKey, UUID> {

    boolean existsByGenerationKey(String generationKey);
}