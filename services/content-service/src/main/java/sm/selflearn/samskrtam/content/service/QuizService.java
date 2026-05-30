package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.dto.QuizSummaryDto;
import sm.selflearn.samskrtam.content.model.QuizType;
import sm.selflearn.samskrtam.content.repository.QuizRepository;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizRepository quizRepository;

    public List<QuizSummaryDto> getQuizzes(QuizType type) {
        return quizRepository.findAll().stream()
                .filter(q -> type == null || q.getQuizType() == type)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private QuizSummaryDto toDto(sm.selflearn.samskrtam.content.model.Quiz quiz) {
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
