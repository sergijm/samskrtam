package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeStatus;

/**
 * Запрос на смену статуса лексемы (task-curriculum-16 §4).
 */
public record LexemeStatusUpdateRequest(
        LexemeStatus status
) {
}