package sm.selflearn.samskrtam.emenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.emenau.model.Answer;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {
}
