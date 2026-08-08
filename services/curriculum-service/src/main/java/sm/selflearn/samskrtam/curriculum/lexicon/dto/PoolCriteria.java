package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import java.util.List;
import java.util.UUID;

/**
 * Фильтры пула кандидатов для lexical-сессий (lexical-quizzes.md §3,
 * task-curriculum-15). Все измерения пересекаются (AND), значения внутри
 * одного измерения объединяются (OR). Все поля опциональны.
 */
public record PoolCriteria(
        List<UUID> topicIds,
        Integer frequencyRankMin,
        Integer frequencyRankMax,
        List<String> posCodes,
        List<String> morphologyClassCodes,
        UUID sourceId,
        String sourceLocationPrefix,
        UUID collectionId,
        UUID excludeMasteredForUserId,
        Integer poolLimit
) {
    public int effectivePoolLimit() {
        return poolLimit == null ? 100 : Math.max(poolLimit, 1);
    }
}
