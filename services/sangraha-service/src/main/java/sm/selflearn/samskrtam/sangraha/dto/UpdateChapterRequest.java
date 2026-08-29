package sm.selflearn.samskrtam.sangraha.dto;

/**
 * Обновление метаданных главы (Chapter). Обновляются только не-null поля.
 */
public record UpdateChapterRequest(
        String titleRu,
        String titleEn,
        String titleSaIast,
        String titleSaDevanagari
) {}
