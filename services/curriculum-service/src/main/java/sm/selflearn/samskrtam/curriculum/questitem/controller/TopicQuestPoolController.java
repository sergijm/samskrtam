package sm.selflearn.samskrtam.curriculum.questitem.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionMatchPayload;

import java.util.List;
import java.util.UUID;

/**
 * Lightweight pool reader for the topic (API v2). Returns the id + itemType + progressTag
 * of every materialized quest item of a topic so quiz-service can run its progress selection
 * (due/new/reserve on {@code quiz_item_score}) before asking to compose by exact ids.
 *
 * <p>Contract-first: see docs/services/curriculum-session-composition.md §5 (step 2).
 */
@RestController
@RequestMapping("/api/v2/curriculum/topics")
@RequiredArgsConstructor
@Tag(name = "Topic Quest-Item Pool", description = "Exposes the materialized quest-item pool of a topic for progress-based selection")
public class TopicQuestPoolController {

    private final TopicRepository topicRepository;
    private final QuestItemRepository questItemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** One pool entry — id + item type + progress tag (no prompt/options payload). */
    public record QuestPoolEntry(UUID id, String itemType, String progressTag) {
    }

    @GetMapping("/{topicCode}/quest-items")
    @Operation(summary = "Fetch the quest-item pool of a topic (id + itemType + progressTag)",
            description = "Returns all materialized quest items of the topic as (id, itemType, progressTag) triples for progress-aware selection")
    public List<QuestPoolEntry> getPool(@PathVariable String topicCode) {
        Topic topic = topicRepository.findByCode(topicCode)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found by code: " + topicCode));
        return questItemRepository.findByTopicId(topic.getId())
                .stream()
                .map(qi -> new QuestPoolEntry(qi.getId(), qi.getItemType(), computeProgressTag(qi)))
                .toList();
    }

    private String computeProgressTag(QuestItem item) {
        String itemType = item.getItemType();
        try {
            switch (itemType) {
                case "DECLENSION_FORM", "DECLENSION_FORM_CHOICE" -> {
                    DeclensionFormPayload p = objectMapper.readValue(item.getPayload(), DeclensionFormPayload.class);
                    return p.caseType() + "|" + p.numberType() + "|" + (p.gender() != null ? p.gender() : "UNSPECIFIED");
                }
                case "CASE_RECOGNITION" -> {
                    CaseRecognitionPayload p = objectMapper.readValue(item.getPayload(), CaseRecognitionPayload.class);
                    return p.correctCaseType() + "|" + p.correctNumberType() + "|" + (p.correctGender() != null ? p.correctGender() : "UNSPECIFIED");
                }
                case "DECLENSION_MATCH" -> {
                    DeclensionMatchPayload p = objectMapper.readValue(item.getPayload(), DeclensionMatchPayload.class);
                    DeclensionMatchPayload.DeclensionMatchPair first =
                            p.pairs() == null || p.pairs().isEmpty() ? null : p.pairs().get(0);
                    if (first == null) return null;
                    return first.caseType() + "|" + first.numberType() + "|UNSPECIFIED";
                }
                default -> {
                    if (item.getCorrectAnswer() != null && !item.getCorrectAnswer().isBlank()) {
                        return item.getCorrectAnswer();
                    }
                    return item.getPrompt();
                }
            }
        } catch (JsonProcessingException e) {
            return item.getPrompt();
        }
    }
}