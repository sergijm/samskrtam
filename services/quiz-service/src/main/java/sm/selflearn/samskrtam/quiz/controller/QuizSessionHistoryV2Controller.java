package sm.selflearn.samskrtam.quiz.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.model.QuizSession;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;
import sm.selflearn.samskrtam.quiz.repository.QuizSessionRepository;
import sm.selflearn.samskrtam.quiz.repository.SessionQuestionRepository;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v2/quiz/sessions")
@RequiredArgsConstructor
public class QuizSessionHistoryV2Controller {

    private final QuizSessionRepository quizSessionRepository;
    private final SessionQuestionRepository sessionQuestionRepository;

    @GetMapping("/{sessionId}")
    public Mono<QuizSession> getSession(@PathVariable UUID sessionId) {
        return quizSessionRepository.findById(sessionId);
    }

    @GetMapping("/{sessionId}/questions")
    public Mono<List<SessionQuestion>> getSessionQuestions(@PathVariable UUID sessionId) {
        return sessionQuestionRepository.findBySessionId(sessionId)
                .collectList();
    }
}