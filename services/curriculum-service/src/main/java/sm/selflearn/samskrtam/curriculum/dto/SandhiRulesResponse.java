package sm.selflearn.samskrtam.curriculum.dto;

import java.util.List;
import java.util.Map;

public record SandhiRulesResponse(
        String topicCode,
        String title,
        List<SandhiRuleDto> rules,
        Map<String, String> categoryGlossary
) {}