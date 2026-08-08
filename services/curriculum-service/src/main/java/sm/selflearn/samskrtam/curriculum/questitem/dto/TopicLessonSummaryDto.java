package sm.selflearn.samskrtam.curriculum.questitem.dto;

import java.util.List;
import java.util.UUID;

/**
 * Lightweight lesson-list row for a v2 topic: what the lesson page picker needs
 * (id + code + localized title + item count).
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