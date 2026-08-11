package sm.selflearn.samskrtam.curriculum.questitem.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questgen.DeclensionQuestItemBatchGenerator;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemDto;
import sm.selflearn.samskrtam.curriculum.questitem.mapper.QuestItemMapper;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @PostMapping("/regenerate")
    public ResponseEntity<Map<String, Integer>> regenerate(@RequestBody RegenerateRequest request) {
        int lexemeLimit = request.lexemeLimit() > 0 ? request.lexemeLimit() : Integer.MAX_VALUE;
        int total = 0;
        for (Topic topic : topicRepository.findAll()) {
            if (topic.isHidden()) {
                continue;
            }
            questItemRepository.deleteByTopicId(topic.getId());
            total += generator.generateForTopic(topic.getId(), lexemeLimit);
        }
        return ResponseEntity.accepted().body(Map.of("generated", total));
    }
}