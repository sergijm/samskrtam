package sm.selflearn.samskrtam.curriculum.questitem.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Lesson read model for a v2 topic: topic metadata plus the set of unique progress tags
 * with their morphology attributes. quiz-service merges this with per-tag progress from
 * {@code quiz_item_score} to build the lesson page.
 */
public record TopicLessonDto(
        UUID topicId,
        String topicCode,
        String titleRu,
        String titleEn,
        String learningLevel,
        Map<String, ProgressTagInfo> tagMetadata
) {
}