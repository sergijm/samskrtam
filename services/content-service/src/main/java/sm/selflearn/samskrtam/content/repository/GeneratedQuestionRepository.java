package sm.selflearn.samskrtam.content.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.model.GeneratedQuestion;

import java.util.List;
import java.util.UUID;

@Repository
public interface GeneratedQuestionRepository extends JpaRepository<GeneratedQuestion, UUID> {
    List<GeneratedQuestion> findByQuizIdAndUserLocale(UUID quizId, String userLocale);
    List<GeneratedQuestion> findByGeneratedQuizDataId(UUID generatedQuizDataId); // New method

    List<GeneratedQuestion> findByGeneratedQuizDataIdOrderByQuestionNumberAsc(UUID generatedQuizDataId);
}
