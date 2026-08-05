package sm.selflearn.samskrtam.curriculum.dto;

import sm.selflearn.samskrtam.curriculum.model.ComplexQuizType;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;

import java.util.UUID;

public record ComplexQuizSummaryDto(
        UUID id,
        ComplexQuizType type,
        LearningLevel learningLevel,
        String titleRu,
        String titleEn,
        Short questionCountHint,
        int topicCount
) {
}
