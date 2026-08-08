package sm.selflearn.samskrtam.curriculum.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Per-user progress state of a topic on the learning map page.
 * Mirrors the frontend `TopicStatus` union in src/config/learnGraph.ts.
 *
 * <p>NOTE: currently generated randomly per request — real progress tracking
 * is an open question (see docs/services/curriculum-service.md §8).
 */
public enum LearnTopicStatus {
    MASTERED,
    IN_PROGRESS,
    RECOMMENDED,
    REVIEW,
    AVAILABLE;

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
}