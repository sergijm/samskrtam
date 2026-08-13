package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Полная лексема + резолвленные таксономии для admin (task-curriculum-16 §2).
 */
public record LexemeDetailDto(
        UUID id,
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
        List<UUID> semanticTopicIds,
        List<LexemeCandidateDto.WordFormDto> wordForms,
        Instant createdAt,
        Instant updatedAt
) {
}