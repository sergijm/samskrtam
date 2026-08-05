package sm.selflearn.samskrtam.curriculum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizTopic;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizTopicId;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplexQuizTopicRepository extends JpaRepository<ComplexQuizTopic, ComplexQuizTopicId> {
    List<ComplexQuizTopic> findByIdComplexQuizId(UUID complexQuizId);

    List<ComplexQuizTopic> findByIdTopicId(UUID topicId);

    long countByIdComplexQuizId(UUID complexQuizId);
}
