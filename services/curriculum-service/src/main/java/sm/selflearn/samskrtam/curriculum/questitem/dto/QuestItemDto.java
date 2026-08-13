package sm.selflearn.samskrtam.curriculum.questitem.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/**
 * Read model of a materialized quest item returned by the v2 quest-items API.
 * {@code correctAnswer} is always {@code null} (and omitted from the serialized
 * JSON) for {@code MATCHING} items — the matching is verified on the backend,
 * see curriculum-quest-items.md §7.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestItemDto(
        UUID id,
        String itemType,
        String answerMode,
        String prompt,
        String promptRu,
        String correctAnswer,
        String correctAnswerRu,
        List<String> distractors,
        List<String> distractorsRu,
        Object payload
) {
}
