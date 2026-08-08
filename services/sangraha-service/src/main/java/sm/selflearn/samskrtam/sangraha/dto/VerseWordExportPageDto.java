package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Ответ экспорта VerseWord[] для batch-импорта лексики (lexicon-content-pipeline.md §2).
 * Одна строка — одно слово одного стиха; постранично курсором по verseId.
 *
 * @param items     словоформы текущей страницы
 * @param nextCursor verseId для следующей страницы, {@code null}, когда строк больше нет
 */
public record VerseWordExportPageDto(
        List<VerseWordExportItemDto> items,
        UUID nextCursor
) {
}