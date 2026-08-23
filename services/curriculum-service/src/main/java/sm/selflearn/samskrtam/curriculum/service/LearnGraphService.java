package sm.selflearn.samskrtam.curriculum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.dto.LearnGraphResponse;
import sm.selflearn.samskrtam.curriculum.dto.LearnLayerDto;
import sm.selflearn.samskrtam.curriculum.dto.LearnTopicDto;
import sm.selflearn.samskrtam.curriculum.dto.LearnTopicStatus;
import sm.selflearn.samskrtam.curriculum.mapper.LearnTopicMapper;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisite;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicPrerequisiteRepository;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.quiz.QuizProgressClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds the learning-map payload for the dashboard page. Layers are grouped
 * by authored LearningLevel (L0..L6) plus one evergreen "always available"
 * layer. Per-user progress is real and sourced from quiz-service
 * (quiz.quiz_item_score) — curriculum-service asks quiz-service for the scores
 * of each topic's progress tags rather than computing progress itself.
 */
@Service
@RequiredArgsConstructor
public class LearnGraphService {

    /**
     * Зеркало {@code ProgressConstants.MASTERED_LOWER_THRESHOLD} из quiz-service
     * (curriculum-service не зависит от модуля quiz, поэтому константа
     * продублирована локально для классификации статуса темы).
     */
    private static final int MASTERED_THRESHOLD = 90;

    private final TopicRepository topicRepository;
    private final TopicPrerequisiteRepository topicPrerequisiteRepository;
    private final LearnTopicMapper learnTopicMapper;
    private final QuestItemRepository questItemRepository;
    private final QuizProgressClient quizProgressClient;

    @Transactional(readOnly = true)
    public LearnGraphResponse getLearnGraph(UUID userId) {
        List<Topic> topics = topicRepository.findAll().stream()
                .filter(t -> !t.isHidden())
                .toList();
        List<TopicPrerequisite> prerequisites = topicPrerequisiteRepository.findAll();

        Map<UUID, Topic> topicById = topics.stream()
                .collect(Collectors.toMap(Topic::getId, topic -> topic));
        Map<UUID, List<String>> prerequisiteCodesByTopic = prerequisites.stream()
                .filter(edge -> topicById.containsKey(edge.getId().getPrerequisiteTopicId()))
                .collect(Collectors.groupingBy(
                        edge -> edge.getId().getTopicId(),
                        Collectors.mapping(
                                edge -> topicById.get(edge.getId().getPrerequisiteTopicId()).getCode(),
                                Collectors.toList())));

        Map<UUID, List<QuestItem>> itemsByTopic = loadItemsByTopic(topics);
        Map<String, Map<String, Integer>> scoresByItemType = loadScores(userId, itemsByTopic);

        return new LearnGraphResponse(buildLayers(topics, prerequisiteCodesByTopic, itemsByTopic, scoresByItemType));
    }

    private Map<UUID, List<QuestItem>> loadItemsByTopic(List<Topic> topics) {
        List<UUID> topicIds = topics.stream().map(Topic::getId).toList();
        if (topicIds.isEmpty()) {
            return Map.of();
        }
        return questItemRepository.findByTopicIdIn(topicIds).stream()
                .collect(Collectors.groupingBy(QuestItem::getTopicId));
    }

    private Map<String, Map<String, Integer>> loadScores(
            UUID userId, Map<UUID, List<QuestItem>> itemsByTopic) {
        Map<String, Set<String>> tagsByItemType = new LinkedHashMap<>();
        for (List<QuestItem> items : itemsByTopic.values()) {
            for (QuestItem qi : items) {
                if (qi.getProgressTag() == null || qi.getProgressTag().isBlank()) {
                    continue;
                }
                tagsByItemType
                        .computeIfAbsent(qi.getItemType(), k -> new LinkedHashSet<>())
                        .add(qi.getProgressTag());
            }
        }
        Map<String, Map<String, Integer>> scoresByItemType = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : tagsByItemType.entrySet()) {
            scoresByItemType.put(
                    entry.getKey(),
                    quizProgressClient.bulkScores(userId, entry.getKey(), List.copyOf(entry.getValue())));
        }
        return scoresByItemType;
    }

    private List<LearnLayerDto> buildLayers(List<Topic> topics,
                                           Map<UUID, List<String>> prerequisiteCodesByTopic,
                                           Map<UUID, List<QuestItem>> itemsByTopic,
                                           Map<String, Map<String, Integer>> scoresByItemType) {
        Comparator<Topic> byOrder = Comparator.comparing(
                        Topic::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Topic::getTitleRu);

        Map<LearningLevel, List<Topic>> byLevel = Arrays.stream(LearningLevel.values())
                .collect(Collectors.toMap(level -> level, level -> new ArrayList<>(), (a, b) -> a, LinkedHashMap::new));
        List<Topic> evergreen = new ArrayList<>();
        for (Topic topic : topics) {
            if (topic.isEvergreen()) {
                evergreen.add(topic);
            } else {
                byLevel.get(topic.getLearningLevel()).add(topic);
            }
        }

        List<LearnLayerDto> layers = new ArrayList<>();
        for (Map.Entry<LearningLevel, List<Topic>> entry : byLevel.entrySet()) {
            entry.getValue().sort(byOrder);
            String lower = entry.getKey().name().toLowerCase();
            layers.add(new LearnLayerDto(
                    entry.getKey().name(),
                    "learnGraph.layers." + lower + ".title",
                    "learnGraph.layers." + lower + ".description",
                    false,
                    toTopics(entry.getValue(), prerequisiteCodesByTopic, itemsByTopic, scoresByItemType)));
        }
        evergreen.sort(byOrder);
        layers.add(new LearnLayerDto(
                "always",
                "learnGraph.layers.always.title",
                "learnGraph.layers.always.description",
                true,
                toTopics(evergreen, prerequisiteCodesByTopic, itemsByTopic, scoresByItemType)));
        return layers;
    }

    private List<LearnTopicDto> toTopics(List<Topic> topics,
                                        Map<UUID, List<String>> prerequisiteCodesByTopic,
                                        Map<UUID, List<QuestItem>> itemsByTopic,
                                        Map<String, Map<String, Integer>> scoresByItemType) {
        return topics.stream()
                .map(topic -> toTopic(topic, prerequisiteCodesByTopic, itemsByTopic, scoresByItemType))
                .toList();
    }

    private LearnTopicDto toTopic(Topic topic,
                                 Map<UUID, List<String>> prerequisiteCodesByTopic,
                                 Map<UUID, List<QuestItem>> itemsByTopic,
                                 Map<String, Map<String, Integer>> scoresByItemType) {
        TopicProgress progress = computeProgress(
                itemsByTopic.getOrDefault(topic.getId(), List.of()), scoresByItemType);
        List<String> prerequisiteCodes = prerequisiteCodesByTopic.getOrDefault(topic.getId(), List.of());
        return learnTopicMapper.toDto(topic, progress.status(), progress.percent(), prerequisiteCodes);
    }

    private TopicProgress computeProgress(List<QuestItem> items,
                                          Map<String, Map<String, Integer>> scoresByItemType) {
        int total = 0;
        int mastered = 0;
        int sum = 0;
        for (QuestItem qi : items) {
            if (qi.getProgressTag() == null || qi.getProgressTag().isBlank()) {
                continue;
            }
            Integer score = scoresByItemType
                    .getOrDefault(qi.getItemType(), Map.of())
                    .get(qi.getProgressTag());
            int sc = score != null ? score : 0;
            total++;
            sum += sc;
            if (sc >= MASTERED_THRESHOLD) {
                mastered++;
            }
        }
        if (total == 0) {
            return new TopicProgress(LearnTopicStatus.AVAILABLE, 0);
        }
        LearnTopicStatus status;
        if (mastered == total) {
            status = LearnTopicStatus.MASTERED;
        } else if (sum > 0) {
            status = LearnTopicStatus.IN_PROGRESS;
        } else {
            status = LearnTopicStatus.AVAILABLE;
        }
        int percent = (int) Math.round((double) sum / total);
        return new TopicProgress(status, percent);
    }

    private record TopicProgress(LearnTopicStatus status, Integer percent) {
    }
}
