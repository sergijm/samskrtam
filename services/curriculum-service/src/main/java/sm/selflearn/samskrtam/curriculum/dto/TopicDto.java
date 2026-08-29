package sm.selflearn.samskrtam.curriculum.dto;

import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TopicDto(
        UUID id,
        String code,
        String titleRu,
        String titleEn,
        LearningLevel learningLevel,
        TopicDomain domain,
        TopicDomainType domainType,
        boolean isEvergreen,
        Short displayOrder,
        List<LearningLevel> appearsInLevels,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
