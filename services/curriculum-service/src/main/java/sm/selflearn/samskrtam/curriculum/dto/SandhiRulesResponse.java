package sm.selflearn.samskrtam.curriculum.dto;

import java.util.List;

public record SandhiRulesResponse(
        String topicCode,
        String title,
        List<SandhiRuleDto> rules
) {}