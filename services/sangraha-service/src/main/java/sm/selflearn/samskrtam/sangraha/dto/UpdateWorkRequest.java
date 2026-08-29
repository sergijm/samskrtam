package sm.selflearn.samskrtam.sangraha.dto;

/**
 * Обновление метаданных произведения (Work). Все поля опциональны; обновляются
 * только переданные (не-null) значения.
 */
public record UpdateWorkRequest(
        String titleRu,
        String titleEn,
        String titleSaIast,
        String titleSaDevanagari,
        String author
) {}
