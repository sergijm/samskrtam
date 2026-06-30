package sm.selflearn.samskrtam.quiz.service.strategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.util.UUID;

@Component
@Slf4j
public class NoopScoreUpdateStrategy implements ScoreUpdateStrategy {

    @Override
    public boolean supports(LessonType lessonType) {
        return true; // fallback for unsupported types
    }

    @Override
    public Mono<Void> updateScore(UUID userId, UUID lessonId, GeneratedQuizQuestionDto generatedQuestion, boolean isCorrect) {
        log.debug("No score update for lessonType, userId={}, lessonId={}, questionId={}", userId, lessonId, generatedQuestion.getId());
        return Mono.empty();
    }
}