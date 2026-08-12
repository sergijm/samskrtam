package sm.selflearn.samskrtam.curriculum.questitem.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questgen.QuizItemGenerationService;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemDto;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemSelectionRequest;
import sm.selflearn.samskrtam.curriculum.questitem.mapper.QuestItemMapper;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Read / regenerate API for materialized quest items (API v2).
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
    private final QuizItemGenerationService generationService;

    /**
     * Selects one quest item per (progress_tag, item_type, answer_mode) group
     * using a window function.  Accepts optional filters for progress tags,
     * item type, answer mode and a limit (0 = no limit).
     */
    @PostMapping("/select")
    public List<QuestItemDto> select(@RequestBody QuestItemSelectionRequest request,
                                     @RequestParam String topicCode) {
        Topic topic = topicRepository.findByCode(topicCode)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + topicCode));

        List<QuestItem> items;
        if (request.progressTags() != null && !request.progressTags().isEmpty()) {
            items = questItemRepository.selectByTopicAndProgressTags(
                    topic.getId(),
                    request.progressTags().toArray(String[]::new),
                    request.itemType(),
                    request.answerMode(),
                    request.limit());
        } else {
            items = questItemRepository.selectByTopic(
                    topic.getId(),
                    request.itemType(),
                    request.answerMode(),
                    request.limit());
        }
        return items.stream()
                .map(questItemMapper::toDto)
                .toList();
    }

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
     * Clears the whole quest-item table and regenerates it for every topic with
     * a registered generator (ADMIN-only, see {@link QuizItemGenerationService}).
     * Returns {@code 202} with per-topic statistics: for each topic, how many
     * quest items were added and how many distinct progress tags it has.
     */
    @PostMapping("/regenerate")
    public ResponseEntity<Map<String, Map<String, Integer>>> regenerate() {
        return ResponseEntity.accepted().body(generationService.regenerate());
    }
}