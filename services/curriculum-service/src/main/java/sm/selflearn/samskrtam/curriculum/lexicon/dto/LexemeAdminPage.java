package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.List;

/**
 * Пагинированный ответ admin-списка лексем (task-curriculum-16 §1).
 */
public record LexemeAdminPage(
        List<LexemeAdminDto> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}