package sm.selflearn.samskrtam.curriculum.dto;

import java.util.List;

/**
 * One collapsible layer of the learning map. Regular layers correspond to a
 * LearningLevel (L0..L6); the evergreen layer groups always-available topics
 * and is flagged with {@code alwaysAvailable}.
 */
public record LearnLayerDto(
        String id,
        String titleRu,
        String titleEn,
        boolean alwaysAvailable,
        List<LearnTopicDto> topics
) {
}