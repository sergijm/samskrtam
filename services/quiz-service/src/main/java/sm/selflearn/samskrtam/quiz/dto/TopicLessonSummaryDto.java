package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID;

/**
 * Client mirror of curriculum-service {@code TopicLessonSummaryDto}
 * (GET /api/v2/curriculum/lessons) — a lightweight lesson-list row used by the
 * lesson picker (id + code + localized title + learning level + item count).
 */
public record TopicLessonSummaryDto(
        UUID id,
        String code,
        String titleRu,
        String titleEn,
        String learningLevel,
        int totalQuestions
) {
}