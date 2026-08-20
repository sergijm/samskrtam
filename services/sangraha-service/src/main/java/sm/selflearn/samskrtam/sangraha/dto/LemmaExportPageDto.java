package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Страница экспорта лемм (lemmas/export). Курсор — {@code lemmaStatistics.id}
 * последней строки страницы. {@code nextCursor == null} → страниц больше нет.
 */
public record LemmaExportPageDto(
        List<LemmaExportItemDto> items,
        UUID nextCursor
) {
}
