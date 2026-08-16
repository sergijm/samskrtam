package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.UUID;

/**
 * Создание/обновление семантического класса (task-curriculum-16 §6).
 */
public record SemanticClassUpsertRequest(
        String code,
        String nameRu,
        String nameEn,
        UUID parentId
) {
}