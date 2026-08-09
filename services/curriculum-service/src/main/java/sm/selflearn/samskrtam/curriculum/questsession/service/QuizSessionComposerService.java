package sm.selflearn.samskrtam.curriculum.questsession.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemDto;
import sm.selflearn.samskrtam.curriculum.questitem.mapper.QuestItemMapper;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.questsession.dto.ComposedQuizItemDto;
import sm.selflearn.samskrtam.curriculum.questsession.dto.QuizSessionComposeRequest;
import sm.selflearn.samskrtam.curriculum.questsession.dto.QuizSessionComposeResponse;
import sm.selflearn.samskrtam.curriculum.questsession.dto.TopicItemSpec;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionMatchPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds a session question sequence from requested topics (see
 * curriculum-session-composition.md §1-2).
 *
 * <p>Universal by design: it operates on the materialized {@link QuestItem} pool of a
 * topic regardless of item type. There is no branching on topic domain or item type
 * here — every item type that has been materialized for a topic participates in the
 * random sample. Adding a new quest type (or a lexical topic once its items are
 * materialized) requires no change to this class.
 */
@Service
@RequiredArgsConstructor
public class QuizSessionComposerService {

    private final TopicRepository topicRepository;
    private final QuestItemRepository questItemRepository;
    private final QuestItemMapper questItemMapper;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Compose a random-ordered sequence of questions per requested topic.
     *
     * <p>Each {@link TopicItemSpec} is satisfied either by a random sample ({@code byCount})
     * or by exact quest-item ids ({@code byIds}, when quiz-service already selected the
     * questions by learning progress). Both modes mix freely in one request.
     *
     * @param request session composition request
     * @return ready-made questions spanning all topics, in random order
     * @throws IllegalArgumentException when the topic list is empty or a topic has no
     *                                  materialized quest items (or a requested id is missing)
     * @throws EntityNotFoundException  when a topic code is unknown
     */
    @Transactional(readOnly = true)
    public QuizSessionComposeResponse compose(QuizSessionComposeRequest request) {
        if (request.topics().isEmpty()) {
            throw new IllegalArgumentException("Session composition requires at least one topic");
        }

        List<ComposedQuizItemDto> unordered = new ArrayList<>();
        for (TopicItemSpec spec : request.topics()) {
            Topic topic = topicRepository.findByCode(spec.topicCode())
                    .orElseThrow(() -> new EntityNotFoundException("Topic not found by code: " + spec.topicCode()));

            List<QuestItem> sample;
            if (spec.hasExplicitIds()) {
                sample = resolveExactIds(topic, spec);
            } else {
                if (spec.count() < 1) {
                    continue;
                }
                sample = questItemRepository.findRandomByTopicId(topic.getId(), spec.count());
            }

            if (sample.isEmpty()) {
                throw new IllegalArgumentException(
                        "No materialized quest items for topic: " + spec.topicCode());
            }
            for (QuestItem item : sample) {
                QuestItemDto dto = questItemMapper.toDto(item);
                String progressTag = computeProgressTag(item);
                unordered.add(new ComposedQuizItemDto(0, spec.topicCode(), dto, progressTag));
            }
        }

        Collections.shuffle(unordered); // random order across all topics

        // Separate FREE_TEXT items: at most 1, always at the end
        List<ComposedQuizItemDto> freeTextItems = unordered.stream()
                .filter(dto -> "FREE_TEXT".equals(dto.item().answerMode()))
                .limit(1)
                .collect(Collectors.toList());
        List<ComposedQuizItemDto> otherItems = unordered.stream()
                .filter(dto -> !"FREE_TEXT".equals(dto.item().answerMode()))
                .collect(Collectors.toList());

        List<ComposedQuizItemDto> ordered = new ArrayList<>(otherItems);
        ordered.addAll(freeTextItems);

        for (int i = 0; i < ordered.size(); i++) {
            ComposedQuizItemDto src = ordered.get(i);
            ordered.set(i, new ComposedQuizItemDto(i + 1, src.topicCode(), src.item(), src.progressTag()));
        }
        return new QuizSessionComposeResponse(List.copyOf(ordered));
    }

    /**
     * Fetches the exact quest-item ids requested for a topic. Ensures every requested id
     * belongs to the topic and exists; a missing id fails fast with a descriptive error
     * (so the caller's progress selection cannot silently drop questions).
     */
    private List<QuestItem> resolveExactIds(Topic topic, TopicItemSpec spec) {
        List<QuestItem> found = questItemRepository.findAllById(spec.itemIds());
        if (found.size() != spec.itemIds().size()) {
            throw new IllegalArgumentException(
                    "Some requested quest items are missing for topic: " + spec.topicCode());
        }
        // validate that every returned item actually belongs to this topic
        for (QuestItem item : found) {
            if (!item.getTopicId().equals(topic.getId())) {
                throw new IllegalArgumentException(
                        "Quest item " + item.getId() + " does not belong to topic: " + spec.topicCode());
            }
        }
        return found;
    }

    /**
     * Computes the progress grouping tag for a quest item.
     * Declension items: {@code caseType|numberType|gender}
     * Other items (vocabulary): {@code correctAnswer} or {@code prompt}
     */
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