package sm.selflearn.samskrtam.eamenau.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "solution_sandhi_rules", schema = "eamenau")
@Data
public class SolutionSandhiRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "solution_id", nullable = false)
    private Integer solutionId;

    @Column(name = "sandhi_rule_id", nullable = false)
    private Integer sandhiRuleId;
}
