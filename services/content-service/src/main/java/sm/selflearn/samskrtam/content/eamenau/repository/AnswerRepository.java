package sm.selflearn.samskrtam.content.eamenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.eamenau.model.Answer;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Integer> {
}
