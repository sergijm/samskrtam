package sm.selflearn.samskrtam.content.dto;

import lombok.Builder;
import lombok.Value;

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
}
