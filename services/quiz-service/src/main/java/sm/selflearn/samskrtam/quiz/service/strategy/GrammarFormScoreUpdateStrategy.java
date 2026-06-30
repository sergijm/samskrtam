package sm.selflearn.samskrtam.quiz.service.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.service.GrammarFormScoreService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
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
        String gender = generatedQuestion.getGender() != null ? generatedQuestion.getGender() : "UNSPECIFIED";
        return grammarFormScoreService.upsertScore(
                userId, lessonId,
                gender,
                generatedQuestion.getTargetCase().name(),
                generatedQuestion.getTargetNumber().name(),
                isCorrect
        ).then();
    }
}