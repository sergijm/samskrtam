package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.config.QuizGeneratorConfig;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.UUID;

/**
 * Единый сервис обновления score/stability для всех типов квизов.
 *
 * <p>Единая таблица прогресса {@code quiz_item_score} (architecture.md §3.6).
 * Использует формулу §2.5 через {@link ScoreCalculator}.
 *
 * <p>Планирование nextReviewAt — временная заглушка (фиксированный интервал),
 * помечена как технический долг. Полноценная SRS-формула — открытый вопрос §6.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizItemScoreService {

    private final QuizItemScoreRepository repository;
    private final QuizGeneratorConfig config;

    /**
     * Обновить score и stability после ответа (upsert).
     *
     * @param userId       идентификатор пользователя
     * @param itemType     тип элемента (VOCABULARY_WORD, DECLENSION_FORM)
     * @param progressTag  тэг группировки прогресса (caseType|numberType|gender или formIast)
     * @param isCorrect    ответ правильный?
     * @return обновлённая строка QuizItemScore
     */
    public Mono<QuizItemScore> upsertScore(
            UUID userId,
            ItemType itemType,
            String progressTag,
            boolean isCorrect) {

        return repository.findByUserIdAndItemTypeAndProgressTag(userId, itemType, progressTag)
                .flatMap(existing -> {
                    ScoreCalculator.Result result = ScoreCalculator.calculate(
                            existing.getScore(),
                            existing.getStability(),
                            existing.getConsecutiveMistakes(),
                            isCorrect,
                            config.getScore());

                    existing.setScore(result.score());
                    existing.setStability(result.stability());
                    existing.setConsecutiveMistakes(result.consecutiveMistakes());
                    existing.setLastAnsweredAt(Instant.now());
                    if (!isCorrect) {
                        existing.setLastMistakeAt(Instant.now());
                    }
                    existing.setNextReviewAt(computeNextReview(existing, result));
                    existing.setUpdatedAt(Instant.now());
                    return repository.save(existing);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Первый ответ — нет строки в БД (prevScore=0, prevStability=1)
                    ScoreCalculator.Result result = ScoreCalculator.calculate(
                            0, 1, 0, isCorrect, config.getScore());

                    Instant now = Instant.now();
                    QuizItemScore newScore = QuizItemScore.builder()
                            .userId(userId)
                            .itemType(itemType)
                            .progressTag(progressTag)
                            .score(result.score())
                            .stability(result.stability())
                            .consecutiveMistakes(result.consecutiveMistakes())
                            .lastAnsweredAt(now)
                            .lastMistakeAt(isCorrect ? null : now)
                            .nextReviewAt(computeNextReviewForNew(result, now))
                            .updatedAt(now)
                            .build();
                    return repository.save(newScore);
                }));
    }

    /**
     * Временная заглушка для nextReviewAt.
     * Правильный ответ: +1 день. Неправильный: +1 час.
     * TODO: заменить полноценной SRS-формулой (открытый вопрос §6).
     */
    private Instant computeNextReview(QuizItemScore existing, ScoreCalculator.Result result) {
        Instant now = Instant.now();
        // Если stability сброшена (серия ошибок) — показать скорее
        if (result.stabilityReset()) {
            return now.plusSeconds(3600); // +1 час
        }
        // Базовая заглушка: +24h для правильного, +1h для неправильного
        return now.plusSeconds(result.consecutiveMistakes() == 0 ? 86400 : 3600);
    }

    private Instant computeNextReviewForNew(ScoreCalculator.Result result, Instant now) {
        if (result.consecutiveMistakes() > 0) {
            return now.plusSeconds(3600); // ошибка → показать через час
        }
        return now.plusSeconds(86400); // правильно → через день
    }
}