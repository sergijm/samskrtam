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
import sm.selflearn.samskrtam.curriculum.repository.TopicPrerequisiteRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Builds the learning-map payload for the dashboard page. Layers are grouped
 * by authored LearningLevel (L0..L6) plus one evergreen "always available"
 * layer. Per-user progress is currently random — real progress tracking is an
 * open question (docs/services/curriculum-service.md §8).
 */
@Service
@RequiredArgsConstructor
public class LearnGraphService {

    private final TopicRepository topicRepository;
    private final TopicPrerequisiteRepository topicPrerequisiteRepository;
    private final LearnTopicMapper learnTopicMapper;

    @Transactional(readOnly = true)
    public LearnGraphResponse getLearnGraph() {
        List<Topic> topics = topicRepository.findAll();
        List<TopicPrerequisite> prerequisites = topicPrerequisiteRepository.findAll();

        Map<UUID, Topic> topicById = topics.stream()
                .collect(Collectors.toMap(Topic::getId, topic -> topic));
        Map<UUID, List<String>> prerequisiteCodesByTopic = prerequisites.stream()
                .collect(Collectors.groupingBy(
                        edge -> edge.getId().getTopicId(),
                        Collectors.mapping(
                                edge -> topicById.get(edge.getId().getPrerequisiteTopicId()).getCode(),
                                Collectors.toList())));

        return new LearnGraphResponse(buildLayers(topics, prerequisiteCodesByTopic));
    }

    private List<LearnLayerDto> buildLayers(List<Topic> topics, Map<UUID, List<String>> prerequisiteCodesByTopic) {
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
                    toTopics(entry.getValue(), prerequisiteCodesByTopic)));
        }
        evergreen.sort(byOrder);
        layers.add(new LearnLayerDto(
                "always",
                "learnGraph.layers.always.title",
                "learnGraph.layers.always.description",
                true,
                toTopics(evergreen, prerequisiteCodesByTopic)));
        return layers;
    }

    private List<LearnTopicDto> toTopics(List<Topic> topics, Map<UUID, List<String>> prerequisiteCodesByTopic) {
        return topics.stream()
                .map(topic -> toTopic(topic, prerequisiteCodesByTopic))
                .toList();
    }

    private LearnTopicDto toTopic(Topic topic, Map<UUID, List<String>> prerequisiteCodesByTopic) {
        LearnTopicStatus status = randomStatus();
        Integer progress = randomProgress(status);
        List<String> prerequisiteCodes = prerequisiteCodesByTopic.getOrDefault(topic.getId(), List.of());
        return learnTopicMapper.toDto(topic, status, progress, prerequisiteCodes);
    }

    private LearnTopicStatus randomStatus() {
        LearnTopicStatus[] values = LearnTopicStatus.values();
        return values[ThreadLocalRandom.current().nextInt(values.length)];
    }

    private Integer randomProgress(LearnTopicStatus status) {
        if (status == LearnTopicStatus.MASTERED) {
            return 100;
        }
        if (status == LearnTopicStatus.AVAILABLE) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(1, 100);
    }
}