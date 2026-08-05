package sm.selflearn.samskrtam.curriculum.repository;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findByCode(String code);

    boolean existsByCode(String code);

    List<Topic> findByLearningLevel(LearningLevel level, Sort sort);

    long countByLearningLevel(LearningLevel level);
}
