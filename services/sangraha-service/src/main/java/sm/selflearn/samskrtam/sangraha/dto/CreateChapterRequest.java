package sm.selflearn.samskrtam.sangraha.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO для POST /works/{workSlug}/chapters.
 * Принимает сырой title и опциональный orderIndex от пользователя.
 * Все остальные поля Chapter (titleRu/En/SaIast/SaDevanagari, slug) заполняются
 * автоматически: детекция языка + LLM tool calling + IAST→SLP1 конвертер для slug.
 * Если orderIndex не передан — backend ставит следующий по порядку в рамках work.
 */
public record CreateChapterRequest(
    @NotBlank
    String title,
    Integer orderIndex
) {}