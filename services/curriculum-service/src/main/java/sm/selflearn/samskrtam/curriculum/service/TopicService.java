package sm.selflearn.samskrtam.curriculum.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.dto.AddPrerequisiteRequest;
import sm.selflearn.samskrtam.curriculum.dto.CreateTopicRequest;
import sm.selflearn.samskrtam.curriculum.dto.GraphLayerDto;
import sm.selflearn.samskrtam.curriculum.dto.LevelSummaryDto;
import sm.selflearn.samskrtam.curriculum.dto.TopicDetailDto;
import sm.selflearn.samskrtam.curriculum.dto.TopicDto;
import sm.selflearn.samskrtam.curriculum.dto.TopicGraphResponse;
import sm.selflearn.samskrtam.curriculum.dto.TopicPrerequisiteDto;
import sm.selflearn.samskrtam.curriculum.dto.UpdateTopicRequest;
import sm.selflearn.samskrtam.curriculum.exception.DuplicateCodeException;
import sm.selflearn.samskrtam.curriculum.exception.TopicCycleException;
import sm.selflearn.samskrtam.curriculum.mapper.TopicMapper;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisite;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisiteId;
import sm.selflearn.samskrtam.curriculum.repository.TopicPrerequisiteRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.curriculum.service.TopicGraphService.TopicGraphResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TopicService {

    private final TopicRepository topicRepository;
    private final TopicPrerequisiteRepository topicPrerequisiteRepository;
    private final TopicGraphService topicGraphService;
    private final ComplexQuizService complexQuizService;
    private final TopicMapper topicMapper;

    public List<TopicDto> listTopics(boolean includeEvergreen, TopicDomain domain, TopicDomainType domainType) {
        return topicRepository.findAll().stream()
                .filter(topic -> !topic.isHidden())
                .filter(topic -> includeEvergreen || !topic.isEvergreen())
                .filter(topic -> domain == null || topic.getDomain() == domain)
                .filter(topic -> domainType == null || topic.getDomainType() == domainType)
                .map(topicMapper::toDto)
                .toList();
    }

    public TopicDetailDto getTopic(UUID id) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + id));
        return new TopicDetailDto(withAppearsInLevels(topic), resolvePrerequisites(id));
    }

    @Transactional
    public TopicDto createTopic(CreateTopicRequest request) {
        if (topicRepository.existsByCode(request.code())) {
            throw new DuplicateCodeException("Topic with code '" + request.code() + "' already exists");
        }
        Topic topic = topicMapper.toEntity(request);
        return topicMapper.toDto(topicRepository.save(topic));
    }

    @Transactional
    public TopicDto updateTopic(UUID id, UpdateTopicRequest request) {
        Topic topic = topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + id));
        topic.setTitleRu(request.titleRu());
        topic.setTitleEn(request.titleEn());
        topic.setLearningLevel(request.learningLevel());
        if (request.isEvergreen() != null) {
            topic.setEvergreen(request.isEvergreen());
        }
        topic.setDisplayOrder(request.displayOrder());
        return topicMapper.toDto(topic);
    }

    @Transactional
    public void deleteTopic(UUID id) {
        topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + id));
        topicRepository.deleteById(id);
    }

    public List<TopicPrerequisiteDto> listPrerequisites(UUID id) {
        topicRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + id));
        return resolvePrerequisites(id);
    }

    @Transactional
    public TopicPrerequisiteDto addPrerequisite(UUID id, AddPrerequisiteRequest request) {
        if (!topicRepository.existsById(id)) {
            throw new EntityNotFoundException("Topic not found: " + id);
        }
        UUID prerequisiteTopicId = request.prerequisiteTopicId();
        if (!topicRepository.existsById(prerequisiteTopicId)) {
            throw new EntityNotFoundException("Prerequisite topic not found: " + prerequisiteTopicId);
        }
        if (id.equals(prerequisiteTopicId)) {
            throw new TopicCycleException("Self-loop prerequisite is not allowed");
        }
        if (topicGraphService.wouldCreateCycle(id, prerequisiteTopicId)) {
            throw new TopicCycleException("Adding prerequisite would create a cycle");
        }

        TopicPrerequisiteId key = new TopicPrerequisiteId();
        key.setTopicId(id);
        key.setPrerequisiteTopicId(prerequisiteTopicId);
        TopicPrerequisite edge = new TopicPrerequisite();
        edge.setId(key);
        edge.setStrength(request.strength());
        TopicPrerequisite saved = topicPrerequisiteRepository.save(edge);
        Topic prerequisite = topicRepository.findById(prerequisiteTopicId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Prerequisite topic not found: " + prerequisiteTopicId));
        return new TopicPrerequisiteDto(topicMapper.toDto(prerequisite), saved.getStrength());
    }

    @Transactional
    public void removePrerequisite(UUID id, UUID prerequisiteTopicId) {
        TopicPrerequisiteId key = new TopicPrerequisiteId();
        key.setTopicId(id);
        key.setPrerequisiteTopicId(prerequisiteTopicId);
        if (!topicPrerequisiteRepository.existsById(key)) {
            throw new EntityNotFoundException("Prerequisite relation not found: "
                    + id + " -> " + prerequisiteTopicId);
        }
        topicPrerequisiteRepository.deleteById(key);
    }

    public TopicGraphResponse getGraph() {
        List<Topic> allTopics = topicRepository.findAll().stream()
                .filter(t -> !t.isHidden())
                .toList();
        List<TopicPrerequisite> allEdges = topicPrerequisiteRepository.findAll();
        TopicGraphResult result = topicGraphService.computeLayers(allTopics, allEdges);

        List<GraphLayerDto> layers = result.layers().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new GraphLayerDto(
                        entry.getKey(),
                        entry.getValue().stream().map(topicMapper::toDto).toList()))
                .toList();
        List<TopicDto> evergreen = result.evergreen().stream()
                .map(topicMapper::toDto)
                .toList();
        return new TopicGraphResponse(layers, evergreen);
    }

    public List<LevelSummaryDto> listLevels() {
        return Arrays.stream(LearningLevel.values())
                .map(level -> new LevelSummaryDto(level, (int) topicRepository.countByLearningLevel(level)))
                .toList();
    }

    public TopicDto getTopicByCode(String code) {
        Topic topic = topicRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found with code: " + code));
        return topicMapper.toDto(topic);
    }

    public List<TopicDto> listTopicsByLevel(LearningLevel level) {
        Sort sort = Sort.by(Sort.Order.asc("displayOrder").nullsLast(), Sort.Order.asc("titleRu"));
        return topicRepository.findByLearningLevel(level, sort).stream()
                .filter(t -> !t.isHidden())
                .map(topicMapper::toDto)
                .toList();
    }

    private TopicDto withAppearsInLevels(Topic topic) {
        TopicDto base = topicMapper.toDto(topic);
        return new TopicDto(
                base.id(), base.code(), base.titleRu(), base.titleEn(), base.learningLevel(),
                base.domain(), base.domainType(),
                base.isEvergreen(), base.displayOrder(),
                complexQuizService.resolveAppearsInLevels(topic.getId(), topic.getLearningLevel()),
                base.createdAt(), base.updatedAt());
    }

    private List<TopicPrerequisiteDto> resolvePrerequisites(UUID topicId) {
        List<TopicPrerequisite> edges = topicPrerequisiteRepository.findByIdTopicId(topicId);
        if (edges.isEmpty()) {
            return List.of();
        }
        List<UUID> prerequisiteIds = edges.stream()
                .map(edge -> edge.getId().getPrerequisiteTopicId())
                .toList();
        Map<UUID, Topic> prerequisiteById = topicRepository.findAllById(prerequisiteIds).stream()
                .collect(Collectors.toMap(Topic::getId, topic -> topic));
        return edges.stream()
                .map(edge -> new TopicPrerequisiteDto(
                        topicMapper.toDto(prerequisiteById.get(edge.getId().getPrerequisiteTopicId())),
                        edge.getStrength()))
                .toList();
    }
}
