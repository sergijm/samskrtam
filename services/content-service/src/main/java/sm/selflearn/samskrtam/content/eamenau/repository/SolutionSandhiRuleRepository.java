package sm.selflearn.samskrtam.content.eamenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.content.eamenau.model.SolutionSandhiRule;
import sm.selflearn.samskrtam.content.eamenau.model.SolutionSandhiRuleId;

@Repository
public interface SolutionSandhiRuleRepository extends JpaRepository<SolutionSandhiRule, SolutionSandhiRuleId> {
}
