package sm.selflearn.samskrtam.content.eamenau.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "solution_sandhi_rules", schema = "eamenau")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolutionSandhiRule {
    @EmbeddedId
    private SolutionSandhiRuleId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("solutionId")
    @JoinColumn(name = "solution_id", nullable = false)
    private Solution solution;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("sandhiRuleId")
    @JoinColumn(name = "sandhi_rule_id", nullable = false)
    private SandhiRule sandhiRule;

    @Column(name = "position_order")
    private Integer positionOrder = 0;
}
