package sm.selflearn.samskrtam.emenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.emenau.model.Solution;

import java.util.List;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Integer> {
    List<Solution> findByTaskIdAndIsCorrect(Integer taskId, boolean isCorrect);
}
