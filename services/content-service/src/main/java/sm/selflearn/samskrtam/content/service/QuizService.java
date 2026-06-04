package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Import Slf4j
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.QuizType;
import sm.selflearn.samskrtam.content.repository.QuizRepository;
import java.util.List;
import java.util.stream.Collectors;

// Импорт DTO из shared:quiz-content-dtos
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.content.model.Quiz; // Импорт Quiz из модели content-service

@Service
@RequiredArgsConstructor
@Slf4j // Add Slf4j annotation
public class QuizService {

    private final QuizRepository quizRepository;

    public List<QuizSummaryDto> getQuizzes(QuizType type) {
        log.debug("getQuizzes called with type: {}", type); // Logging argument
        return quizRepository.findAll().stream()
                .filter(q -> type == null || q.getQuizType() == type)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public QuizSummaryDto getQuizBySlug(String slug) {
        log.debug("getQuizBySlug called with slug: {}", slug); // Logging argument
        return quizRepository.findBySlug(slug)
                .map(this::toDto)
                .orElseThrow(() -> new SamskrtamException("QUIZ_NOT_FOUND", "Quiz not found with slug: " + slug));
    }

    private QuizSummaryDto toDto(Quiz quiz) { // Используем Quiz из content-service.model
        var dto = new QuizSummaryDto();
        dto.setId(quiz.getId());
        dto.setSlug(quiz.getSlug());
        dto.setTitleRu(quiz.getTitleRu());
        dto.setTitleEn(quiz.getTitleEn());
        dto.setQuizType(quiz.getQuizType());
        dto.setDifficulty(quiz.getDifficulty());
        return dto;
    }
}
