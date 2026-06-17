package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

@Value
@Builder
public class SandhiRuleDto {
    Integer id;
    Integer ruleNumber;
    String ruleType;
    String shortDescription;
    String whitneyNumber;
    String iastExample;
    String hkExample;
    String notes;
    String fullText;
    Set<SandhiRuleGroupDto> sandhiRuleGroups;
}
