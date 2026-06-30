package sm.selflearn.samskrtam.emenau.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import sm.selflearn.samskrtam.emenau.model.SandhiRule;

import java.util.List;
import java.util.Optional;

@Repository
public interface SandhiRuleRepository extends JpaRepository<SandhiRule, Integer> {
    List<SandhiRule> findAllByOrderByRuleNumberAsc();
    List<SandhiRule> findByRuleNumberIn(List<Integer> ruleNumbers);
    Optional<SandhiRule> findByRuleNumber(Integer ruleNumber);
}
