package sm.selflearn.samskrtam.sangraha.dto;

import java.time.Instant;
import java.util.List;

public record VerseAnalysisDto(
    String translationRu,
    String translationEn,
    List<SandhiSplitDto> sandhiSplits,
    String modelName,
    Instant analyzedAt
) {
    public record SandhiSplitDto(
        String surface,
        List<String> components,
        List<Integer> ruleNumbers
    ) {}
}