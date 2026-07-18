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
 * Status-filtered quiz item generation (§3, §4 п.«2а»).
 * Отбирает элементы только из указанного бакета (NEW/LEARNING/REVIEW), не смешивая due/new/reserve.
 * Выделен из QuizGenerator для компактности.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizStatusFilteredGenerator {

    private final QuizItemScoreRepository quizItemScoreRepository;
    private final QuizGeneratorConfig config;

    /**
     * Отобрать список {@link QuizItem} для сессии с ручным фильтром по бакету.
     * Не смешивает due/new/reserve — отбирает только из указанного бакета.
     *
     * @return Mono со списком QuizItem; если пул пуст — Mono.empty() (→ 404)
     */
    public Mono<List<QuizItem>> generate(
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

    // ================== Private generators ==================

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
     * Делегирует в {@link DueItemPriorityComparator}.
     */
    private List<QuizItemScore> sortScoresByPriority(
            List<QuizItemScore> scores,
            QuizGeneratorConfig.DueSortParams params,
            Instant now) {
        return DueItemPriorityComparator.sortByPriority(scores, params, now);
    }
}
