package sm.selflearn.samskrtam.sangraha.dto;

/**
 * DTO для PUT /chapters/{chapterId}.
 * Те же поля, что и CreateChapterRequest: title (для LLM-перевода) и опциональный orderIndex.
 * Если title не передан — обновляется только orderIndex.
 * Если orderIndex не передан — он не меняется.
 */
public record UpdateChapterRequest(
    String title,
    Integer orderIndex
) {}