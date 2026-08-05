package sm.selflearn.samskrtam.curriculum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisite;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisiteId;

import java.util.List;
import java.util.UUID;

@Repository
public interface TopicPrerequisiteRepository extends JpaRepository<TopicPrerequisite, TopicPrerequisiteId> {
    List<TopicPrerequisite> findByIdTopicId(UUID topicId);

    List<TopicPrerequisite> findByIdPrerequisiteTopicId(UUID prerequisiteTopicId);
}
