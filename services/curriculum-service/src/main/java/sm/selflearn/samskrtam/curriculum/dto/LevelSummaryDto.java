package sm.selflearn.samskrtam.curriculum.dto;

import sm.selflearn.samskrtam.curriculum.model.LearningLevel;

public record LevelSummaryDto(
        LearningLevel level,
        int topicCount
) {
}
