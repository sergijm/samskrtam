package sm.selflearn.samskrtam.curriculum.questitem.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.questgen.DeclensionQuestItemBatchGenerator;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemDto;
import sm.selflearn.samskrtam.curriculum.questitem.mapper.QuestItemMapper;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read / regenerate API for materialized quest items (API v2), see
 * curriculum-quest-items.md §6.
 */
@RestController
@RequestMapping("/api/v2/curriculum/quest-items")
@RequiredArgsConstructor
public class QuestItemController {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;

    private final QuestItemRepository questItemRepository;
    private final TopicRepository topicRepository;
    private final QuestItemMapper questItemMapper;
    private final DeclensionQuestItemBatchGenerator generator;

    /**
     * Returns a random sample of ready-made quest items of the requested type for
     * a topic (random ordering via {@code ORDER BY random()}).
     */
    @GetMapping
    public List<QuestItemDto> getQuestItems(
            @RequestParam UUID topicId,
            @RequestParam String itemType,
            @RequestParam(defaultValue = "20") int limit) {
        if (!topicRepository.existsById(topicId)) {
            throw new EntityNotFoundException("Topic not found: " + topicId);
        }
        int capped = Math.max(1, Math.min(limit, MAX_LIMIT));
        return questItemRepository.findRandomByTopicIdAndItemType(topicId, itemType, capped)
                .stream()
                .map(questItemMapper::toDto)
                .toList();
    }

    /**
     * Deletes the already materialized items of the requested type for a topic
     * and regenerates them (ADMIN-only). Returns {@code 202} with the number of
     * newly generated items.
     */
    @PostMapping("/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Integer>> regenerate(
            @RequestParam UUID topicId,
            @RequestParam String itemType) {
        int generated = regenerateForType(topicId, itemType);
        return ResponseEntity.accepted().body(Map.of("generated", generated));
    }

    private int regenerateForType(UUID topicId, String itemType) {
        if (!isDeclensionType(itemType)) {
            throw new IllegalArgumentException("Unsupported quest itemType: " + itemType);
        }
        topicRepository.findById(topicId)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + topicId));
        questItemRepository.deleteByTopicIdAndItemType(topicId, itemType);
        return switch (itemType) {
            case "DECLENSION_FORM", "DECLENSION_FORM_CHOICE" -> generator.generateFormsForTopic(topicId);
            case "CASE_RECOGNITION" -> generator.generateCaseRecognitionForTopic(topicId);
            default -> generator.generateMatchForTopic(topicId); // DECLENSION_MATCH
        };
    }

    private boolean isDeclensionType(String itemType) {
        return switch (itemType) {
            case "DECLENSION_FORM", "DECLENSION_FORM_CHOICE", "CASE_RECOGNITION", "DECLENSION_MATCH" -> true;
            default -> false;
        };
    }
}
