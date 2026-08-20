package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.List;
import java.util.UUID;

/**
 * Семантический класс (для дерева) — широкий админ-представление
 * (task-curriculum-16 §6, GET /semantic-classes/tree).
 */
public record SemanticClassNodeDto(
        UUID id,
        String code,
        String nameRu,
        String nameEn,
        UUID parentId,
        List<SemanticClassNodeDto> children
) {
}