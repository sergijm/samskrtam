package sm.selflearn.samskrtam.eamenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.eamenau.model.SolutionSandhiRule;

import java.util.List;

@Repository
public interface SolutionSandhiRuleRepository extends JpaRepository<SolutionSandhiRule, Integer> {
    List<SolutionSandhiRule> findBySolutionId(Integer solutionId);
    void deleteBySolutionId(Integer solutionId);
    void deleteBySolutionIdAndSandhiRuleIdIn(Integer solutionId, List<Integer> sandhiRuleIds);
}
