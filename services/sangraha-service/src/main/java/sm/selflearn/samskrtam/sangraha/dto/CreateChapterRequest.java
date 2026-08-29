package sm.selflearn.samskrtam.sangraha.dto;

/**
 * Создание главы (Chapter) внутри произведения. titleRu обязательно.
 */
public record CreateChapterRequest(
        String titleRu,
        String titleEn,
        String titleSaIast,
        String titleSaDevanagari
) {}
