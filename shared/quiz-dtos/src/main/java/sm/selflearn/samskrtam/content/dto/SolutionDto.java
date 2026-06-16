package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SolutionDto {
    private Integer id;
    private String solutionText;
    private String stepByStep;
    private List<SandhiRuleInfo> sandhiRules;
}
