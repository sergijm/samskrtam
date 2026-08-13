package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID;

/**
 * Client mirror of curriculum-service {@code TopicDto}
 * (GET /api/v2/curriculum/topics).
 */
public record TopicDto(
        UUID id,
        String code,
        String titleRu,
        String titleEn,
        String learningLevel,
        String domain,
        boolean isEvergreen
) {
}