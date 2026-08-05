package sm.selflearn.samskrtam.curriculum.dto;

import sm.selflearn.samskrtam.curriculum.model.ComplexQuizType;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record ComplexQuizDto(
        UUID id,
        ComplexQuizType type,
        LearningLevel learningLevel,
        String titleRu,
        String titleEn,
        Short questionCountHint,
        List<TopicDto> topics,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
