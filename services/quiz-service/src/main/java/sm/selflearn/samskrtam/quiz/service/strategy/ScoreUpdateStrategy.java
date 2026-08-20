package sm.selflearn.samskrtam.quiz.service.strategy;

import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;

import java.util.UUID;

public interface ScoreUpdateStrategy {

    boolean supports(LessonType lessonType);

    Mono<Void> updateScore(UUID userId, UUID lessonId, GeneratedQuizQuestionDto generatedQuestion, boolean isCorrect);
}