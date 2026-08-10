package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Страница экспорта лемм (lemmas/export). Курсор — последний lemmaStatistics.id.
 * nextCursor == null → страниц больше нет.
 */
public record LemmaExportPageDto(
        List<LemmaExportItemDto> items,
        UUID nextCursor
) {
}
