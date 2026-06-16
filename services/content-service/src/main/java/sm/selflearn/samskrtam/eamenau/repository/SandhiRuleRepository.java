package sm.selflearn.samskrtam.eamenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.eamenau.model.SandhiRule;

import java.util.List;

@Repository
public interface SandhiRuleRepository extends JpaRepository<SandhiRule, Integer> {
    List<SandhiRule> findAllByOrderByRuleNumberAsc();
    List<SandhiRule> findByRuleNumberIn(List<Integer> ruleNumbers);
}
