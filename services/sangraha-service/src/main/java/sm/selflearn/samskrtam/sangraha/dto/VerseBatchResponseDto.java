package sm.selflearn.samskrtam.sangraha.dto;

import java.util.List;

/**
 * Ответ GET /api/v1/sangraha/verse (sangraha-service/batch-verse-review.md).
 * Не найденные/удалённые id просто отсутствуют в verses — не ошибка.
 */
public record VerseBatchResponseDto(
        List<VerseBatchItemDto> verses
) {}
