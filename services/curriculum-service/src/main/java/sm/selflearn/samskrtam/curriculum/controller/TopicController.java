package sm.selflearn.samskrtam.curriculum.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.dto.AddPrerequisiteRequest;
import sm.selflearn.samskrtam.curriculum.dto.CreateTopicRequest;
import sm.selflearn.samskrtam.curriculum.dto.LevelSummaryDto;
import sm.selflearn.samskrtam.curriculum.dto.TopicDetailDto;
import sm.selflearn.samskrtam.curriculum.dto.TopicDto;
import sm.selflearn.samskrtam.curriculum.dto.TopicGraphResponse;
import sm.selflearn.samskrtam.curriculum.dto.TopicPrerequisiteDto;
import sm.selflearn.samskrtam.curriculum.dto.UpdateTopicRequest;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.service.TopicService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/curriculum")
@RequiredArgsConstructor
public class TopicController {

    private final TopicService topicService;

    @GetMapping("/topics")
    public List<TopicDto> listTopics(
            @RequestParam(defaultValue = "true") boolean includeEvergreen,
            @RequestParam(required = false) TopicDomain domain,
            @RequestParam(required = false) TopicDomainType domainType) {
        return topicService.listTopics(includeEvergreen, domain, domainType);
    }

    @GetMapping("/topics/{id}")
    public TopicDetailDto getTopic(@PathVariable UUID id) {
        return topicService.getTopic(id);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicDto createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return topicService.createTopic(request);
    }

    @PutMapping("/topics/{id}")
    public TopicDto updateTopic(@PathVariable UUID id, @Valid @RequestBody UpdateTopicRequest request) {
        return topicService.updateTopic(id, request);
    }

    @DeleteMapping("/topics/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTopic(@PathVariable UUID id) {
        topicService.deleteTopic(id);
    }

    @GetMapping("/topics/{id}/prerequisites")
    public List<TopicPrerequisiteDto> listPrerequisites(@PathVariable UUID id) {
        return topicService.listPrerequisites(id);
    }

    @PostMapping("/topics/{id}/prerequisites")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicPrerequisiteDto addPrerequisite(
            @PathVariable UUID id,
            @Valid @RequestBody AddPrerequisiteRequest request) {
        return topicService.addPrerequisite(id, request);
    }

    @DeleteMapping("/topics/{id}/prerequisites/{prerequisiteTopicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePrerequisite(
            @PathVariable UUID id,
            @PathVariable UUID prerequisiteTopicId) {
        topicService.removePrerequisite(id, prerequisiteTopicId);
    }

    @GetMapping("/graph")
    public TopicGraphResponse getGraph() {
        return topicService.getGraph();
    }

    @GetMapping("/levels")
    public List<LevelSummaryDto> listLevels() {
        return topicService.listLevels();
    }

    @GetMapping("/levels/{level}/topics")
    public List<TopicDto> listTopicsByLevel(@PathVariable LearningLevel level) {
        return topicService.listTopicsByLevel(level);
    }
}
