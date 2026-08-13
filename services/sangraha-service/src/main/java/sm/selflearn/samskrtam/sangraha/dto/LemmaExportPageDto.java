package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;

/**
 * Страница экспорта лемм (lemmas/export). Курсор — keyset по
 * {@code occurrenceCount:lemmaStatisticsId} последней строки страницы.
 * {@code nextCursor == null} → страниц больше нет.
 */
public record LemmaExportPageDto(
        List<LemmaExportItemDto> items,
        String nextCursor
) {
}
