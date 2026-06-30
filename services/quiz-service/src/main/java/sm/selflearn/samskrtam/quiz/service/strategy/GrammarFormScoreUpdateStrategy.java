package sm.selflearn.samskrtam.quiz.service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.service.GrammarFormScoreService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GrammarFormScoreUpdateStrategy implements ScoreUpdateStrategy {

    private final GrammarFormScoreService grammarFormScoreService;

    @Override
    public boolean supports(LessonType lessonType) {
        return lessonType == LessonType.DECLENSIONS || lessonType == LessonType.CONJUGATIONS;
    }

    @Override
    public Mono<Void> updateScore(UUID userId, UUID lessonId, GeneratedQuizQuestionDto generatedQuestion, boolean isCorrect) {
        if (generatedQuestion.getTargetCase() == null || generatedQuestion.getTargetNumber() == null) {
            return Mono.empty();
        }
        return grammarFormScoreService.upsertScore(
                userId, lessonId,
                generatedQuestion.getTargetCase().name(),
                generatedQuestion.getTargetNumber().name(),
                isCorrect
        ).then();
    }
}