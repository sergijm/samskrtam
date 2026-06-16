package sm.selflearn.samskrtam.eamenau.model;

import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolutionSandhiRuleId implements Serializable {
    private Integer solutionId;
    private Integer sandhiRuleId;
}
