package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import sm.selflearn.samskrtam.curriculum.lexicon.model.SourceKind;

/**
 * Создание/обновление источника корпуса (task-curriculum-16 §7).
 */
public record SourceUpsertRequest(
        String code,
        String titleRu,
        String titleEn,
        SourceKind kind,
        String externalSangrahaWorkSlug
) {
}