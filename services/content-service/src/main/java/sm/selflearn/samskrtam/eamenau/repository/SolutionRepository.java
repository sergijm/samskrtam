package sm.selflearn.samskrtam.eamenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.eamenau.model.Solution;

import java.util.List;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Integer> {
    List<Solution> findByTaskIdAndIsCorrect(Integer taskId, boolean isCorrect);
}
