package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;
import java.util.UUID;

/**
 * Узел классификатора произведений (works_class), рекурсивный для дропдауна
 * с множественным выбором.
 */
public record WorksClassTreeNodeDto(
    UUID id,
    UUID parentId,
    String code,
    String titleRu,
    String titleEn,
    String titleSaIast,
    String titleSaDeva,
    int sortOrder,
    int workCount,
    List<WorksClassTreeNodeDto> children
) {

    public WorksClassTreeNodeDto {
        children = children == null ? List.of() : children;
    }
}