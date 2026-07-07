package sm.selflearn.samskrtam.quiz.service.strategy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.dto.LessonType;

/**
 * Упрощённый реестр стратегий: все типы теперь используют единую {@link QuizItemScoreUpdateStrategy}.
 * Оставлен для обратной совместимости — {@link SessionOperationsService} теперь напрямую
 * инжектит {@link QuizItemScoreUpdateStrategy}.
 *
 * @deprecated В будущем удалить; все вызовы заменить на прямую инъекцию QuizItemScoreUpdateStrategy.
 */
@Deprecated
@Component
@RequiredArgsConstructor
public class ScoreUpdateStrategyRegistry {

    private final QuizItemScoreUpdateStrategy unifiedStrategy;

    public ScoreUpdateStrategy getStrategy(LessonType lessonType) {
        return unifiedStrategy;
    }
}