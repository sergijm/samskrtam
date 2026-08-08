package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeStatus;

import java.util.UUID;

/**
 * Список лексем (пагинированный) для admin-UI (task-curriculum-16 §1).
 */
public record LexemeAdminDto(
        UUID id,
        String lemmaIast,
        String lemmaDevanagari,
        String lemmaSlp1,
        String glossRu,
        String glossEn,
        String gender,
        LexemeStatus status,
        Integer frequencyRank,
        int wordFormCount,
        boolean hasSemanticTopic
) {
}