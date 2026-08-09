package sm.selflearn.samskrtam.curriculum.questitem.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionMatchPayload;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.dto.ProgressTagInfo;
import sm.selflearn.samskrtam.curriculum.questitem.dto.TopicLessonDto;
import sm.selflearn.samskrtam.curriculum.questitem.dto.TopicLessonItemDto;
import sm.selflearn.samskrtam.curriculum.questitem.dto.TopicLessonSummaryDto;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builds the lesson read model for a topic (API v2): topic metadata + the morphology
 * attributes of every materialized quest item, parsed from the JSONB payload. This is
 * the curriculum-side source for the grammar lesson page — content-service is removed.
 */
@Service
@RequiredArgsConstructor
public class TopicLessonService {

    private final TopicRepository topicRepository;
    private final QuestItemRepository questItemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Transactional(readOnly = true)
    public List<TopicLessonSummaryDto> listLessons() {
        return topicRepository.findAll().stream()
                .filter(t -> !t.isHidden())
                .map(this::toSummary)
                .toList();
    }

    private TopicLessonSummaryDto toSummary(Topic topic) {
        int distinctCells = (int) questItemRepository.findByTopicId(topic.getId())
                .stream()
                .map(this::toItemDto)
                .filter(item -> item.caseType() != null && item.numberType() != null)
                .map(item -> item.caseType() + ":" + item.numberType())
                .distinct()
                .count();
        return new TopicLessonSummaryDto(
                topic.getId(), topic.getCode(), topic.getTitleRu(), topic.getTitleEn(),
                topic.getLearningLevel() != null ? topic.getLearningLevel().name() : null,
                distinctCells);
    }

    @Transactional(readOnly = true)
    public TopicLessonDto getLesson(String topicCode) {
        Topic topic = topicRepository.findByCode(topicCode)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found by code: " + topicCode));
        Map<String, ProgressTagInfo> tagMetadata = questItemRepository.findByTopicId(topic.getId())
                .stream()
                .map(this::toProgressTagEntry)
                .filter(e -> e != null)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> a));
        return new TopicLessonDto(
                topic.getId(), topic.getCode(), topic.getTitleRu(), topic.getTitleEn(),
                topic.getLearningLevel() != null ? topic.getLearningLevel().name() : null,
                tagMetadata);
    }

    /**
     * Computes the progress tag and metadata for a quest item. Returns null for items
     * that cannot produce a tag (unknown types without morphology data).
     */
    private Map.Entry<String, ProgressTagInfo> toProgressTagEntry(QuestItem qi) {
        String itemType = qi.getItemType();
        switch (itemType) {
            case "DECLENSION_FORM", "DECLENSION_FORM_CHOICE" -> {
                DeclensionFormPayload p = parsePayload(qi, DeclensionFormPayload.class);
                String tag = p.caseType() + "|" + p.numberType() + "|" + (p.gender() != null ? p.gender() : "UNSPECIFIED");
                return Map.entry(tag, new ProgressTagInfo(itemType,
                        p.gender(), p.caseType(), p.numberType(), p.correctFormIast()));
            }
            case "CASE_RECOGNITION" -> {
                CaseRecognitionPayload p = parsePayload(qi, CaseRecognitionPayload.class);
                String tag = p.correctCaseType() + "|" + p.correctNumberType() + "|" + (p.correctGender() != null ? p.correctGender() : "UNSPECIFIED");
                return Map.entry(tag, new ProgressTagInfo(itemType,
                        p.correctGender(), p.correctCaseType(), p.correctNumberType(), p.wordFormIast()));
            }
            case "DECLENSION_MATCH" -> {
                DeclensionMatchPayload p = parsePayload(qi, DeclensionMatchPayload.class);
                DeclensionMatchPayload.DeclensionMatchPair first =
                        p.pairs() == null || p.pairs().isEmpty() ? null : p.pairs().get(0);
                if (first == null) return null;
                String tag = first.caseType() + "|" + first.numberType() + "|UNSPECIFIED";
                return Map.entry(tag, new ProgressTagInfo(itemType,
                        null, first.caseType(), first.numberType(), null));
            }
            default -> {
                // Vocabulary or unknown type: use formIast as tag if available
                String tag = qi.getPrompt();
                if (tag == null || tag.isBlank()) return null;
                return Map.entry(tag, new ProgressTagInfo(itemType,
                        null, null, null, tag));
            }
        }
    }

    private TopicLessonItemDto toItemDto(QuestItem qi) {
        String itemType = qi.getItemType();
        switch (itemType) {
            case "DECLENSION_FORM", "DECLENSION_FORM_CHOICE" -> {
                DeclensionFormPayload p = parsePayload(qi, DeclensionFormPayload.class);
                return new TopicLessonItemDto(qi.getId(), itemType,
                        p.gender(), p.caseType(), p.numberType(),
                        p.correctFormIast(), qi.getPrompt());
            }
            case "CASE_RECOGNITION" -> {
                CaseRecognitionPayload p = parsePayload(qi, CaseRecognitionPayload.class);
                return new TopicLessonItemDto(qi.getId(), itemType,
                        p.correctGender(), p.correctCaseType(), p.correctNumberType(),
                        p.wordFormIast(), qi.getPrompt());
            }
            case "DECLENSION_MATCH" -> {
                DeclensionMatchPayload p = parsePayload(qi, DeclensionMatchPayload.class);
                DeclensionMatchPayload.DeclensionMatchPair first =
                        p.pairs() == null || p.pairs().isEmpty() ? null : p.pairs().get(0);
                return new TopicLessonItemDto(qi.getId(), itemType,
                        null,
                        first != null ? first.caseType() : null,
                        first != null ? first.numberType() : null,
                        null, qi.getPrompt());
            }
            default -> {
                return new TopicLessonItemDto(qi.getId(), itemType,
                        null, null, null, null, qi.getPrompt());
            }
        }
    }

    private <T> T parsePayload(QuestItem qi, Class<T> type) {
        try {
            return objectMapper.readValue(qi.getPayload(), type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to parse payload of quest item " + qi.getId() + " as " + type.getSimpleName(), e);
        }
    }
}