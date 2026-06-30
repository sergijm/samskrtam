package sm.selflearn.samskrtam.emenau.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "solution_sandhi_rules", schema = "eamenau")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SolutionSandhiRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "solution_id", nullable = false)
    private Integer solutionId;

    @Column(name = "sandhi_rule_id", nullable = false)
    private Integer sandhiRuleId;
}
