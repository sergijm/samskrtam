package sm.selflearn.samskrtam.quiz.dto;

import java.util.UUID;

/**
 * Client mirror of curriculum-service {@code TopicLessonItemDto}.
 */
public record TopicLessonItemDto(
        UUID id,
        String itemType,
        String gender,
        String caseType,
        String numberType,
        String formIast,
        String prompt
) {
}