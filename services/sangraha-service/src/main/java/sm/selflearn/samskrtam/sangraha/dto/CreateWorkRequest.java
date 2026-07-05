package sm.selflearn.samskrtam.sangraha.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO для POST /works.
 * Принимает сырой title и опциональное description от пользователя.
 * Все остальные поля Work (titleRu/En/SaIast/SaDevanagari, author, slug) заполняются
 * автоматически: детекция языка + LLM tool calling + IAST→SLP1 конвертер для slug.
 */
public record CreateWorkRequest(
    @NotBlank
    String title,
    String description
) {}