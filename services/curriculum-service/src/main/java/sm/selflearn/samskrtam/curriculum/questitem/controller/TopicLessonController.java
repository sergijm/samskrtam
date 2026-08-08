package sm.selflearn.samskrtam.curriculum.questitem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.questitem.dto.TopicLessonDto;
import sm.selflearn.samskrtam.curriculum.questitem.dto.TopicLessonSummaryDto;
import sm.selflearn.samskrtam.curriculum.questitem.service.TopicLessonService;

import java.util.List;

/**
 * Lesson read model for a v2 topic (API v2). Returns topic metadata + the morphology
 * attributes of its quest items so quiz-service can build the grammar-lesson page with
 * per-item progress (content-service is removed).
 */
@RestController
@RequestMapping("/api/v2/curriculum")
@RequiredArgsConstructor
public class TopicLessonController {

    private final TopicLessonService topicLessonService;

    @GetMapping("/lessons")
    public List<TopicLessonSummaryDto> listLessons() {
        return topicLessonService.listLessons();
    }

    @GetMapping("/topics/{topicCode}/lesson")
    public TopicLessonDto getLesson(@PathVariable String topicCode) {
        return topicLessonService.getLesson(topicCode);
    }
}