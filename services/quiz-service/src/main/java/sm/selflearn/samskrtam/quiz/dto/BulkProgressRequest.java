package sm.selflearn.samskrtam.quiz.dto;

import java.util.List;

/**
 * Запрос пакетного получения оценок прогресса по конкретному itemType.
 */
public record BulkProgressRequest(
        String itemType,
        List<String> progressTags) {
}
