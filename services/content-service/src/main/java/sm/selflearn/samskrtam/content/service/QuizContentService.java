package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import sm.selflearn.samskrtam.content.dto.QuizListItemResponse;
import sm.selflearn.samskrtam.content.model.Quiz;
import sm.selflearn.samskrtam.content.repository.QuizRepository; // Import QuizRepository

import java.util.Locale; // Import Locale for title selection

@Service
@RequiredArgsConstructor
public class QuizContentService {

    private final QuizRepository quizRepository; // Inject QuizRepository

    public Flux<QuizListItemResponse> getQuizList() {
        return Flux.fromIterable(quizRepository.findAll()) // Fetch all quizzes from DB
                .map(this::mapToQuizListItemResponse);
    }

    private QuizListItemResponse mapToQuizListItemResponse(Quiz quiz) {
        // Determine title and description based on locale if needed, for now use default
        // For simplicity, let's assume we always return English titles for now,
        // or we can pass a locale parameter to getQuizList if needed.
        // For now, using titleEn for description as well, as description is not in Quiz entity
        // and totalQuestions is questionsPerSession
        return QuizListItemResponse.builder()
                .id(quiz.getId())
                .title(quiz.getTitleEn()) // Using English title
                .description(quiz.getTitleEn()) // Using English title as description for now
                .quizType(quiz.getQuizType())
                .slug(quiz.getSlug())
                .totalQuestions(quiz.getQuestionsPerSession())
                .build();
    }
}
