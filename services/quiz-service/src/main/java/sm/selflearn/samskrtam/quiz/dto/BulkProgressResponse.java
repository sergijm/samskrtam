package sm.selflearn.samskrtam.quiz.dto;

import java.util.Map;

/**
 * Ответ пакетного получения оценок: прогресс-тег → оценка (0..100).
 * Теги без записи в {@code quiz_item_score} отсутствуют в карте (считаются 0).
 */
public record BulkProgressResponse(
        String itemType,
        Map<String, Integer> scores) {
}
