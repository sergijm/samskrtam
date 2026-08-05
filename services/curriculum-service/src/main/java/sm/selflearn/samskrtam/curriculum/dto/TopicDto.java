package sm.selflearn.samskrtam.curriculum.dto;

import sm.selflearn.samskrtam.curriculum.model.LearningLevel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TopicDto(
        UUID id,
        String code,
        String titleRu,
        String titleEn,
        LearningLevel learningLevel,
        boolean isEvergreen,
        Short displayOrder,
        List<LearningLevel> appearsInLevels,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
