package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.UUID;

/**
 * Создание/обновление семантической темы (task-curriculum-16 §6).
 */
public record SemanticTopicUpsertRequest(
        String code,
        String nameRu,
        String nameEn,
        UUID parentId
) {
}