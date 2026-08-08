package sm.selflearn.samskrtam.quiz.dto;

import java.util.Map;
import java.util.UUID;

/**
 * Client mirror of curriculum-service {@code TopicLessonDto} (GET /api/v2/curriculum/topics/{code}/lesson).
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