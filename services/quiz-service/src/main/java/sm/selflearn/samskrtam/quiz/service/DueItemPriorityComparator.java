package sm.selflearn.samskrtam.quiz.service;

import sm.selflearn.samskrtam.quiz.config.QuizGeneratorConfig;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Чистая логика приоритизации due-элементов.
 * Используется {@link QuizGenerator} и {@link QuizProgressTagSetGenerator}.
 *
 * <p>Поддерживает стратегии:
 * <ul>
 *   <li>OVERDUE_FIRST — сначала самые просроченные</li>
 *   <li>LOWEST_SCORE_FIRST — сначала с наименьшим score</li>
 *   <li>WEIGHTED — взвешенная формула overdue + score + mistakes</li>
 * </ul>
 */
public final class DueItemPriorityComparator {

    private DueItemPriorityComparator() {
        // утилитный класс
    }

    /**
     * Компаратор due-элементов по приоритету (на основе карты refId → QuizItemScore).
     */
    public static Comparator<UUID> comparingByPriority(
            Map<UUID, QuizItemScore> scoreMap,
            QuizGeneratorConfig.DueSortParams params,
            Instant now) {
        return (a, b) -> {
            return switch (params.getDueSortStrategy().toUpperCase()) {
                case "OVERDUE_FIRST" -> {
                    long overdueA = getOverdueSeconds(a, scoreMap, now);
                    long overdueB = getOverdueSeconds(b, scoreMap, now);
                    yield Long.compare(overdueB, overdueA); // более просроченный — выше
                }
                case "LOWEST_SCORE_FIRST" -> {
                    int scoreA = scoreMap.containsKey(a) ? scoreMap.get(a).getScore() : 0;
                    int scoreB = scoreMap.containsKey(b) ? scoreMap.get(b).getScore() : 0;
                    yield Integer.compare(scoreA, scoreB); // меньший score — выше
                }
                default -> { // WEIGHTED
                    double pa = computeWeightedPriority(scoreMap.get(a), params, now);
                    double pb = computeWeightedPriority(scoreMap.get(b), params, now);
                    yield Double.compare(pb, pa); // больший вес — выше
                }
            };
        };
    }

    /**
     * Компаратор для списка QuizItemScore (без карты UUID→Score).
     */
    public static Comparator<QuizItemScore> comparingScoresByPriority(
            QuizGeneratorConfig.DueSortParams params,
            Instant now) {
        return (a, b) -> {
            double pa = computeWeightedPriority(a, params, now);
            double pb = computeWeightedPriority(b, params, now);
            return Double.compare(pb, pa);
        };
    }

    /**
     * Сортировка списка QuizItemScore по weighted priority.
     */
    public static List<QuizItemScore> sortByPriority(
            List<QuizItemScore> scores,
            QuizGeneratorConfig.DueSortParams params,
            Instant now) {
        List<QuizItemScore> mutable = new ArrayList<>(scores);
        mutable.sort(comparingScoresByPriority(params, now));
        return mutable;
    }

    /**
     * Взвешенная приоритизация: sum(overdueWeight * overdueRatio + scoreWeight * scoreRatio + mistakeWeight * mistakeRatio).
     */
    public static double computeWeightedPriority(
            QuizItemScore score,
            QuizGeneratorConfig.DueSortParams params,
            Instant now) {
        if (score == null) {
            return 0.0;
        }

        double priority = 0.0;

        // Фактор просроченности
        if (score.getNextReviewAt() != null) {
            long overdueSeconds = now.getEpochSecond() - score.getNextReviewAt().getEpochSecond();
            if (overdueSeconds > 0) {
                priority += params.getOverdueWeight() * Math.min(overdueSeconds / 86400.0, 30.0);
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

    private static long getOverdueSeconds(UUID refId, Map<UUID, QuizItemScore> scoreMap, Instant now) {
        QuizItemScore score = scoreMap.get(refId);
        if (score == null || score.getNextReviewAt() == null) {
            return 0;
        }
        return now.getEpochSecond() - score.getNextReviewAt().getEpochSecond();
    }
}
