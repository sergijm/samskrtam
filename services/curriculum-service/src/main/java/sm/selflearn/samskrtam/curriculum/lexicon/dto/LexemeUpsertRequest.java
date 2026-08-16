package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;

import java.util.List;
import java.util.UUID;

/**
 * Создание/обновление лексемы ADMIN (task-curriculum-16 §3).
 */
public record LexemeUpsertRequest(
        String lemmaIast,
        String lemmaDevanagari,
        String lemmaSlp1,
        String glossRu,
        String glossEn,
        String longDefinitionRu,
        String longDefinitionEn,
        LexemeGender gender,
        List<String> posCodes,
        List<String> morphologyClassCodes,
        List<UUID> semanticClassIds
) {
}