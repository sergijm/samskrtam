package sm.selflearn.samskrtam.curriculum.lexicon.dto;

/**
 * Запрос на обновление прогресса одной лексемы (task-curriculum-15 §9).
 */
public record LexemeProgressUpdateRequest(
        boolean correct
) {
}