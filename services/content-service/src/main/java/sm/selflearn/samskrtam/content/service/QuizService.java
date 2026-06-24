package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.content.model.Lesson;
import sm.selflearn.samskrtam.content.repository.QuizRepository;
import sm.selflearn.samskrtam.content.dto.LessonType; // Corrected import

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizService {

    private final QuizRepository quizRepository;

    public List<QuizSummaryDto> getQuizzes(LessonType type) {
        log.debug("getQuizzes called with type: {}", type);
        return quizRepository.findAll().stream()
                .filter(q -> type == null || q.getLessonType() == type)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public QuizSummaryDto getQuizBySlug(String slug) {
        log.debug("getQuizBySlug called with slug: {}", slug);
        return quizRepository.findBySlug(slug)
                .map(this::toDto)
                .orElseThrow(() -> new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found with slug: " + slug));
    }

    public QuizSummaryDto getQuizSummaryById(UUID quizId) {
        log.debug("getQuizSummaryById called with quizId: {}", quizId);
        return quizRepository.findById(quizId)
                .map(this::toDto)
                .orElseThrow(() -> new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found with ID: " + quizId));
    }

    private QuizSummaryDto toDto(Lesson lesson) {
        var dto = new QuizSummaryDto();
        dto.setId(lesson.getId());
        dto.setSlug(lesson.getSlug());
        dto.setTitleRu(lesson.getTitleRu());
        dto.setTitleEn(lesson.getTitleEn());
        dto.setDescriptionRu(lesson.getDescriptionRu()); // Populate new field
        dto.setDescriptionEn(lesson.getDescriptionEn()); // Populate new field
        dto.setLessonType(lesson.getLessonType());
        dto.setDifficulty(lesson.getDifficulty());
        return dto;
    }


}
