package sm.selflearn.samskrtam.curriculum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisite;
import sm.selflearn.samskrtam.curriculum.repository.TopicPrerequisiteRepository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicGraphService {

    private final TopicPrerequisiteRepository topicPrerequisiteRepository;

    public boolean wouldCreateCycle(UUID topicId, UUID prerequisiteTopicId) {
        if (topicId.equals(prerequisiteTopicId)) {
            return true;
        }
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(prerequisiteTopicId);
        while (!stack.isEmpty()) {
            UUID current = stack.pop();
            if (!visited.add(current)) {
                continue;
            }
            if (current.equals(topicId)) {
                return true;
            }
            for (TopicPrerequisite edge : topicPrerequisiteRepository.findByIdTopicId(current)) {
                stack.push(edge.getId().getPrerequisiteTopicId());
            }
        }
        return false;
    }

    public TopicGraphResult computeLayers(List<Topic> allTopics, List<TopicPrerequisite> allEdges) {
        List<Topic> evergreen = new ArrayList<>();
        Map<UUID, Topic> graphTopicById = new HashMap<>();
        for (Topic topic : allTopics) {
            if (topic.isEvergreen()) {
                evergreen.add(topic);
            } else {
                graphTopicById.put(topic.getId(), topic);
            }
        }

        Map<UUID, Integer> inDegree = new HashMap<>();
        Map<UUID, List<UUID>> dependents = new HashMap<>();
        for (TopicPrerequisite edge : allEdges) {
            UUID topicId = edge.getId().getTopicId();
            UUID prerequisiteTopicId = edge.getId().getPrerequisiteTopicId();
            if (!graphTopicById.containsKey(topicId) || !graphTopicById.containsKey(prerequisiteTopicId)) {
                continue;
            }
            inDegree.put(topicId, inDegree.getOrDefault(topicId, 0) + 1);
            dependents.computeIfAbsent(prerequisiteTopicId, k -> new ArrayList<>()).add(topicId);
        }

        Deque<UUID> ready = new ArrayDeque<>();
        for (UUID topicId : graphTopicById.keySet()) {
            if (!inDegree.containsKey(topicId)) {
                ready.add(topicId);
            }
        }

        Map<UUID, Integer> layerByTopic = new HashMap<>();
        int processed = 0;
        while (!ready.isEmpty()) {
            UUID current = ready.poll();
            processed++;
            int currentLayer = layerByTopic.getOrDefault(current, 0);
            for (UUID dependent : dependents.getOrDefault(current, List.of())) {
                layerByTopic.merge(dependent, currentLayer + 1, Math::max);
                int remaining = inDegree.merge(dependent, -1, Integer::sum);
                if (remaining == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (processed != graphTopicById.size()) {
            throw new IllegalStateException("Cycle detected in topic_prerequisite graph; "
                    + (graphTopicById.size() - processed) + " topics unreachable by topological order");
        }

        Map<Integer, List<Topic>> layers = new LinkedHashMap<>();
        for (Topic topic : graphTopicById.values()) {
            int layer = layerByTopic.getOrDefault(topic.getId(), 0);
            layers.computeIfAbsent(layer, k -> new ArrayList<>()).add(topic);
        }
        Comparator<Topic> withinLayer = Comparator.comparing(
                        Topic::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Topic::getTitleRu);
        for (List<Topic> topicsInLayer : layers.values()) {
            topicsInLayer.sort(withinLayer);
        }

        return new TopicGraphResult(layers, evergreen);
    }

    public record TopicGraphResult(Map<Integer, List<Topic>> layers, List<Topic> evergreen) {
    }
}
