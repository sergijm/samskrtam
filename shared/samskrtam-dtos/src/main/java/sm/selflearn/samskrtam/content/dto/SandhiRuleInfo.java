package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SandhiRuleInfo {
    private int ruleNumber;
    private String shortDescription;
}
