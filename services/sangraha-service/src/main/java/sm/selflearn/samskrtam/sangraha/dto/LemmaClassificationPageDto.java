package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Страница списка на ревью (lemma-classification.md §4).
 */
public record LemmaClassificationPageDto(
        List<LemmaClassificationItemDto> items,
        UUID nextCursor) {
}