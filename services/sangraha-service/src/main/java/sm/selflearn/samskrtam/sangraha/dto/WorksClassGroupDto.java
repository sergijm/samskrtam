package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;

/**
 * Группа классификатора: все корни одного {@code classification} значения
 * (например GENRE/ERA) для одного дропдауна с множественным выбором.
 */
public record WorksClassGroupDto(
    String classification,
    List<WorksClassTreeNodeDto> classes
) {}