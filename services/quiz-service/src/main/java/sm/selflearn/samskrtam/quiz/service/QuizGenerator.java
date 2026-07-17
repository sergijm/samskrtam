package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.config.QuizGeneratorConfig;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItem;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.model.StatusFilter;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Универсальный генератор отбора вопросов квиза.
 * Инвариантен к {@link ItemType} — не ветвится по типу внутри алгоритма §4.
 *
 * <p>Алгоритм:
 * <ol>
 *   <li>Получить список externalRefId для scope через ContentClient (вызывающий код)</li>
 *   <li>Присоединить строки quiz_item_score; разделить на due/new/reserve</li>
 *   <li>Приоритизировать due по весовой формуле</li>
 *   <li>Отобрать new не более maxNewPerSession</li>
 *   <li>Добить reserve если нужно</li>
 *   <li>Перемешать с учётом interleaveCategories и minGap</li>
 * </ol>
 *
 * @see <a href="docs/quizzes/quiz-generator-spec.md#section-4">Спецификация §4</a>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGenerator {

    private final QuizItemScoreRepository quizItemScoreRepository;
    private final QuizGeneratorConfig config;

    /**
     * Отобрать список {@link QuizItem} для сессии.
     *
     * @param userId          идентификатор пользователя
     * @param itemType        тип элементов (VOCABULARY_WORD, DECLENSION_FORM)
     * @param externalRefIds  список externalRefId в scope (получен от ContentClient)
     * @return список QuizItem, отсортированный для показа
     */
    public Mono<List<QuizItem>> generate(
            UUID userId,
            ItemType itemType,
            List<UUID> externalRefIds) {

        if (externalRefIds == null || externalRefIds.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        return quizItemScoreRepository
                .findByUserIdAndItemTypeAndExternalRefIdIn(userId, itemType, externalRefIds)
                .collectList()
                .map(scores -> buildSelection(userId, itemType, externalRefIds, scores));
    }

    /**
     * Основной алгоритм отбора (чистая логика, без БД).
     *
     * @param userId         идентификатор пользователя
     * @param itemType       тип элементов
     * @param allRefIds      полный список externalRefId в scope
     * @param existingScores уже имеющиеся строки quiz_item_score для этого userId+itemType+scope
     * @return список QuizItem для сессии
     */
    List<QuizItem> buildSelection(
            UUID userId,
            ItemType itemType,
            List<UUID> allRefIds,
            List<QuizItemScore> existingScores) {

        QuizGeneratorConfig.SessionSizeParams sessionSizeParams = config.getSessionSize();
        QuizGeneratorConfig.DueSortParams dueSortParams = config.getDueSort();
        QuizGeneratorConfig.ReserveParams reserveParams = config.getReserve();
        QuizGeneratorConfig.GeneralParams generalParams = config.getGeneral();
        QuizGeneratorConfig.BucketParams bucketParams = config.getBuckets();

        int sessionSize = sessionSizeParams.getSessionSize();
        int maxNew = sessionSizeParams.getMaxNewPerSession();
        int dueCap = (int) Math.ceil(sessionSize * sessionSizeParams.getDueCapRatio());

        // Map externalRefId → QuizItemScore
        Map<UUID, QuizItemScore> scoreMap = existingScores.stream()
                .collect(Collectors.toMap(QuizItemScore::getExternalRefId, s -> s, (a, b) -> a));

        Instant now = Instant.now();

        // Разделяем на due / new / reserve
        List<UUID> dueItems = new ArrayList<>();
        List<UUID> newItems = new ArrayList<>();
        List<UUID> reserveItems = new ArrayList<>();

        for (UUID refId : allRefIds) {
            QuizItemScore score = scoreMap.get(refId);
            if (score == null) {
                // Нет строки = NEW
                newItems.add(refId);
            } else if (score.getNextReviewAt() != null
                    && !score.getNextReviewAt().isAfter(now)) {
                // nextReviewAt <= now → due
                dueItems.add(refId);
            } else if (ScoreCalculator.determineBucket(true, score.getScore(), bucketParams)
                    == ScoreCalculator.Bucket.MASTERED) {
                // MASTERED → reserve (либо skipped до masteredCooldown)
                // Проверка masteredCooldown: если nextReviewAt + cooldown прошёл — показать
                if (score.getNextReviewAt() != null) {
                    Instant cooldownEnd = score.getNextReviewAt()
                            .plusSeconds(generalParams.getMasteredCooldown() * 86400L);
                    if (!cooldownEnd.isAfter(now)) {
                        reserveItems.add(refId);
                    }
                }
            } else {
                // LEARNING / DIFFICULT, не просрочено → reserve
                reserveItems.add(refId);
            }
        }

        // 3. Приоритизация due
        List<QuizItem> selected = new ArrayList<>();
        dueItems.sort((a, b) -> compareDuePriority(a, b, scoreMap, dueSortParams, now));
        int dueCount = Math.min(dueItems.size(), dueCap);
        for (int i = 0; i < dueCount; i++) {
            selected.add(new QuizItem(itemType, dueItems.get(i)));
        }

        // 4. Добавить new
        int remainingNew = Math.min(newItems.size(), maxNew);
        int roomAfterDue = sessionSize - selected.size();
        int newToTake = Math.min(remainingNew, roomAfterDue);
        // Случайный порядок для new
        Collections.shuffle(newItems);
        for (int i = 0; i < newToTake; i++) {
            selected.add(new QuizItem(itemType, newItems.get(i)));
        }

        // 5. Добить reserve
        int roomAfterNew = sessionSize - selected.size();
        if (roomAfterNew > 0 && reserveParams.isAllowReserveWhenNoDue() && !reserveItems.isEmpty()) {
            Collections.shuffle(reserveItems);
            int reserveToTake = Math.min(reserveItems.size(), roomAfterNew);
            for (int i = 0; i < reserveToTake; i++) {
                selected.add(new QuizItem(itemType, reserveItems.get(i)));
            }
        }

        // 6. Перемешивание с учётом minGapBetweenSameWordRepeats
        // Поскольку в текущей итерации каждый QuizItem уникален в сессии,
        // межкатегорийное перемешивание реализуем простым shuffle-финальным
        if (generalParams.isInterleaveCategories() && selected.size() > 1) {
            Collections.shuffle(selected);
        }

        return selected;
    }

    /**
     * Сравнение due-элементов по приоритету.
     */
    private int compareDuePriority(
            UUID a, UUID b,
            Map<UUID, QuizItemScore> scoreMap,
            QuizGeneratorConfig.DueSortParams params,
            Instant now) {

        return switch (params.getDueSortStrategy().toUpperCase()) {
            case "OVERDUE_FIRST" -> {
                long overdueA = scoreMap.containsKey(a) && scoreMap.get(a).getNextReviewAt() != null
                        ? now.getEpochSecond() - scoreMap.get(a).getNextReviewAt().getEpochSecond()
                        : 0;
                long overdueB = scoreMap.containsKey(b) && scoreMap.get(b).getNextReviewAt() != null
                        ? now.getEpochSecond() - scoreMap.get(b).getNextReviewAt().getEpochSecond()
                        : 0;
                yield Long.compare(overdueB, overdueA); // более просроченный — выше
            }
            case "LOWEST_SCORE_FIRST" -> {
                int scoreA = scoreMap.containsKey(a) ? scoreMap.get(a).getScore() : 0;
                int scoreB = scoreMap.containsKey(b) ? scoreMap.get(b).getScore() : 0;
                yield Integer.compare(scoreA, scoreB); // меньший score — выше
            }
            default -> { // WEIGHTED (по умолчанию)
                double priorityA = computeWeightedPriority(a, scoreMap, params, now);
                double priorityB = computeWeightedPriority(b, scoreMap, params, now);
                yield Double.compare(priorityB, priorityA); // больший вес — выше
            }
        };
    }

        /**
     * Отобрать список {@link QuizItem} для сессии с ручным фильтром по бакету (§4 п.«2а»).
     * Не смешивает due/new/reserve — отбирает только из указанного бакета.
     *
     * @return Mono со списком QuizItem; если пул пуст — Mono.empty() (→ 404)
     */
    public Mono<List<QuizItem>> generateStatusFiltered(
            UUID userId,
            ItemType itemType,
            List<UUID> externalRefIds,
            StatusFilter statusFilter) {

        if (externalRefIds == null || externalRefIds.isEmpty()) {
            return Mono.empty();
        }

        QuizGeneratorConfig.SessionSizeParams sessionSizeParams = config.getSessionSize();
        QuizGeneratorConfig.DueSortParams dueSortParams = config.getDueSort();
        QuizGeneratorConfig.BucketParams bucketParams = config.getBuckets();
        int sessionSize = sessionSizeParams.getSessionSize();

        return switch (statusFilter) {
            case NEW -> generateNewOnly(userId, itemType, externalRefIds, sessionSize);
            case LEARNING -> generateLearningOnly(userId, itemType, externalRefIds, sessionSize,
                    bucketParams, dueSortParams);
            case REVIEW -> generateReviewOnly(userId, itemType, externalRefIds, sessionSize,
                    bucketParams, dueSortParams);
        };
    }

    /**
     * NEW: все externalRefId урока минус уже существующие строки score.
     */
    private Mono<List<QuizItem>> generateNewOnly(
            UUID userId, ItemType itemType, List<UUID> externalRefIds, int sessionSize) {
        return quizItemScoreRepository
                .findByUserIdAndItemTypeAndExternalRefIdIn(userId, itemType, externalRefIds)
                .map(QuizItemScore::getExternalRefId)
                .collectList()
                .map(existingRefIds -> {
                    Set<UUID> existingSet = new HashSet<>(existingRefIds);
                    List<UUID> newRefIds = externalRefIds.stream()
                            .filter(refId -> !existingSet.contains(refId))
                            .collect(Collectors.toList());
                    if (newRefIds.isEmpty()) {
                        return Collections.<QuizItem>emptyList();
                    }
                    Collections.shuffle(newRefIds);
                    int take = Math.min(newRefIds.size(), sessionSize);
                    return newRefIds.subList(0, take).stream()
                            .map(refId -> new QuizItem(itemType, refId))
                            .collect(Collectors.toList());
                });
    }

    /**
     * LEARNING: единицы с существующей строкой, чей бакет LEARNING или DIFFICULT.
     * nextReviewAt не учитывается.
     */
    private Mono<List<QuizItem>> generateLearningOnly(
            UUID userId, ItemType itemType, List<UUID> externalRefIds, int sessionSize,
            QuizGeneratorConfig.BucketParams bucketParams,
            QuizGeneratorConfig.DueSortParams dueSortParams) {
        return quizItemScoreRepository
                .findLearningItems(userId, itemType, externalRefIds,
                        bucketParams.getMasteredLowerThreshold())
                .collectList()
                .map(scores -> {
                    if (scores.isEmpty()) {
                        return Collections.<QuizItem>emptyList();
                    }
                    List<QuizItemScore> sorted = sortScoresByPriority(scores, dueSortParams, Instant.now());
                    int take = Math.min(sorted.size(), sessionSize);
                    return sorted.subList(0, take).stream()
                            .map(s -> new QuizItem(itemType, s.getExternalRefId()))
                            .collect(Collectors.toList());
                });
    }

    /**
     * REVIEW: бакет MASTERED с nextReviewAt ≤ now.
     */
    private Mono<List<QuizItem>> generateReviewOnly(
            UUID userId, ItemType itemType, List<UUID> externalRefIds, int sessionSize,
            QuizGeneratorConfig.BucketParams bucketParams,
            QuizGeneratorConfig.DueSortParams dueSortParams) {
        return quizItemScoreRepository
                .findReviewItems(userId, itemType, externalRefIds,
                        bucketParams.getMasteredLowerThreshold(), Instant.now())
                .collectList()
                .map(scores -> {
                    if (scores.isEmpty()) {
                        return Collections.<QuizItem>emptyList();
                    }
                    List<QuizItemScore> sorted = sortScoresByPriority(scores, dueSortParams, Instant.now());
                    int take = Math.min(sorted.size(), sessionSize);
                    return sorted.subList(0, take).stream()
                            .map(s -> new QuizItem(itemType, s.getExternalRefId()))
                            .collect(Collectors.toList());
                });
    }

    /**
     * Сортировка QuizItemScore по weighted priority (без карты UUID→Score).
     */
    private List<QuizItemScore> sortScoresByPriority(
            List<QuizItemScore> scores,
            QuizGeneratorConfig.DueSortParams params,
            Instant now) {
        List<QuizItemScore> mutable = new ArrayList<>(scores);
        mutable.sort((a, b) -> {
            double pa = computeWeightedPriorityScore(a, params, now);
            double pb = computeWeightedPriorityScore(b, params, now);
            return Double.compare(pb, pa);
        });
        return mutable;
    }

    private double computeWeightedPriorityScore(
            QuizItemScore score,
            QuizGeneratorConfig.DueSortParams params,
            Instant now) {
        if (score == null) {
            return 0.0;
        }
        double priority = 0.0;

        if (score.getNextReviewAt() != null) {
            long overdueSeconds = now.getEpochSecond() - score.getNextReviewAt().getEpochSecond();
            if (overdueSeconds > 0) {
                priority += params.getOverdueWeight() * Math.min(overdueSeconds / 86400.0, 30.0);
            }
        }

        double scoreRatio = 1.0 - (score.getScore() / 100.0);
        priority += params.getScoreWeight() * scoreRatio;

        if (score.getConsecutiveMistakes() > 0) {
            priority += params.getMistakeWeight() * score.getConsecutiveMistakes();
        }

        return priority;
    }

    /**
     * Взвешенная приоритизация: sum(overdueWeight * overdueRatio + scoreWeight * scoreRatio + mistakeWeight * mistakeRatio).
     */
    private double computeWeightedPriority(
            UUID refId,
            Map<UUID, QuizItemScore> scoreMap,
            QuizGeneratorConfig.DueSortParams params,
            Instant now) {

        QuizItemScore score = scoreMap.get(refId);
        if (score == null) {
            return 0.0;
        }

        double priority = 0.0;

        // Фактор просроченности
        if (score.getNextReviewAt() != null) {
            long overdueSeconds = now.getEpochSecond() - score.getNextReviewAt().getEpochSecond();
            if (overdueSeconds > 0) {
                priority += params.getOverdueWeight() * Math.min(overdueSeconds / 86400.0, 30.0); // до 30 дней
            }
        }

        // Фактор низкого score: меньше score → выше приоритет
        double scoreRatio = 1.0 - (score.getScore() / 100.0);
        priority += params.getScoreWeight() * scoreRatio;

        // Фактор недавних ошибок
        if (score.getConsecutiveMistakes() > 0) {
            priority += params.getMistakeWeight() * score.getConsecutiveMistakes();
        }

        return priority;
    }
}