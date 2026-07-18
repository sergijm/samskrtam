package sm.selflearn.samskrtam.quiz.service;

import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.WordStatus;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;

import java.time.Instant;

/**
 * Единый резолвер статуса {@link WordStatus} из {@link QuizItemScore}.
 * Используется как в Vocabulary-, так и в Grammar-прогрессе.
 *
 * <p>Правила (ADR-007):
 * <ul>
 *   <li>Нет записи → {@link WordStatus#NEW}</li>
 *   <li>score &lt; {@link ProgressConstants#MASTERED_LOWER_THRESHOLD} → {@link WordStatus#LEARNING}</li>
 *   <li>score ≥ порог и nextReviewAt в прошлом → {@link WordStatus#REVIEW}</li>
 *   <li>score ≥ порог и nextReviewAt в будущем (или null) → {@link WordStatus#MASTERED}</li>
 * </ul>
 */
@Component
public class WordStatusResolver {

    /**
     * Вычисляет статус по хранимой записи quiz_item_score.
     *
     * @param itemScore запись из quiz_item_score или null
     * @param now       текущее время
     * @return статус по правилам ADR-007
     */
    public WordStatus resolve(QuizItemScore itemScore, Instant now) {
        if (itemScore == null) {
            return WordStatus.NEW;
        }
        if (itemScore.getScore() < ProgressConstants.MASTERED_LOWER_THRESHOLD) {
            return WordStatus.LEARNING;
        }
        // score >= MASTERED_LOWER_THRESHOLD
        if (itemScore.getNextReviewAt() != null && !itemScore.getNextReviewAt().isAfter(now)) {
            return WordStatus.REVIEW;
        }
        return WordStatus.MASTERED;
    }
}
