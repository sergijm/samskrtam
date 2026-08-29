package sm.selflearn.samskrtam.sangraha.dto;

/**
 * Создание произведения (Work). Поле titleRu обязательно; остальные —
 * опциональны и дублируются из titleRu при отсутствии. sourceCode опционален:
 * при отсутствии backend берёт первый доступный источник.
 */
public record CreateWorkRequest(
        String titleRu,
        String titleEn,
        String titleSaIast,
        String titleSaDevanagari,
        String sourceCode
) {}
