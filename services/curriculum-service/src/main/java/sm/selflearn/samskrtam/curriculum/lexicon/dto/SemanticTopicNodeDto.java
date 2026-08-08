package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.List;
import java.util.UUID;

/**
 * Семантическая тема (для дерева) — широкий админ-представление
 * (task-curriculum-16 §6, GET /semantic-topics/tree).
 */
public record SemanticTopicNodeDto(
        UUID id,
        String code,
        String nameRu,
        String nameEn,
        UUID parentId,
        List<SemanticTopicNodeDto> children
) {
}