package sm.selflearn.samskrtam.curriculum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuiz;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizType;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;

import java.util.List;
import java.util.UUID;

@Repository
public interface ComplexQuizRepository extends JpaRepository<ComplexQuiz, UUID> {
    List<ComplexQuiz> findByLearningLevelAndType(LearningLevel level, ComplexQuizType type);

    List<ComplexQuiz> findByLearningLevel(LearningLevel level);

    List<ComplexQuiz> findByType(ComplexQuizType type);
}
