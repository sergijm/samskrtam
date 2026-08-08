package sm.selflearn.samskrtam.curriculum.questitem.dto;

import java.util.UUID;

/**
 * One lesson cell: a materialized quest item of a topic with its morphology attributes
 * parsed from the item payload (gender/case/number + reference form). Serves as the
 * building block for the v2 grammar-lesson progress grid.
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