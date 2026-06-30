package sm.selflearn.samskrtam.quiz.service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.GeneratedQuizQuestionDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.service.WordScoreService;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VocabularyScoreUpdateStrategy implements ScoreUpdateStrategy {

    private final WordScoreService wordScoreService;

    @Override
    public boolean supports(LessonType lessonType) {
        return LessonType.isVocabulary(lessonType);
    }

    @Override
    public Mono<Void> updateScore(UUID userId, UUID lessonId, GeneratedQuizQuestionDto generatedQuestion, boolean isCorrect) {
        if (generatedQuestion.getVocabularyWordId() == null) {
            return Mono.empty();
        }
        return wordScoreService.upsertScore(userId, generatedQuestion.getVocabularyWordId(), lessonId, isCorrect).then();
    }
}