package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.config.QuizGeneratorConfig;
import sm.selflearn.samskrtam.quiz.dto.QuestPoolItemDto;
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
            List<QuestPoolItemDto> pool,
            StatusFilter statusFilter) {

        if (pool == null || pool.isEmpty()) {
            return Mono.empty();
        }

        QuizGeneratorConfig.SessionSizeParams sessionSizeParams = config.getSessionSize();
        QuizGeneratorConfig.DueSortParams dueSortParams = config.getDueSort();
        QuizGeneratorConfig.BucketParams bucketParams = config.getBuckets();
        int sessionSize = sessionSizeParams.getSessionSize();

        // Group pool items by progressTag for lookup
        Map<String, List<UUID>> tagToIds = pool.stream()
                .filter(p -> p.progressTag() != null && !p.progressTag().isBlank())
                .collect(Collectors.groupingBy(
                        QuestPoolItemDto::progressTag,
                        LinkedHashMap::new,
                        Collectors.mapping(QuestPoolItemDto::id, Collectors.toList())));

        List<String> tags = new ArrayList<>(tagToIds.keySet());

        return switch (statusFilter) {
            case NEW -> generateNewOnly(userId, itemType, pool, sessionSize, tags, tagToIds);
            case LEARNING -> generateLearningOnly(userId, itemType, sessionSize, tags, tagToIds,
                    bucketParams, dueSortParams);
            case REVIEW -> generateReviewOnly(userId, itemType, sessionSize, tags, tagToIds,
                    bucketParams, dueSortParams);
        };
    }

    private Mono<List<QuizItem>> generateNewOnly(
            UUID userId, ItemType itemType, List<QuestPoolItemDto> pool, int sessionSize,
            List<String> tags, Map<String, List<UUID>> tagToIds) {

        if (tags.isEmpty()) {
            // All items are new (no tags)
            return Mono.just(takeRandom(pool, sessionSize, itemType));
        }

        return quizItemScoreRepository
                .findByUserIdAndItemTypeAndProgressTagIn(userId, itemType, tags)
                .map(QuizItemScore::getProgressTag)
                .collectList()
                .map(existingTags -> {
                    Set<String> existingSet = new HashSet<>(existingTags);
                    List<UUID> newIds = new ArrayList<>();
                    // Tags without scores → new
                    for (Map.Entry<String, List<UUID>> entry : tagToIds.entrySet()) {
                        if (!existingSet.contains(entry.getKey())) {
                            newIds.addAll(entry.getValue());
                        }
                    }
                    // Also include untagged items
                    List<UUID> untagged = pool.stream()
                            .filter(p -> p.progressTag() == null || p.progressTag().isBlank())
                            .map(QuestPoolItemDto::id)
                            .collect(Collectors.toList());
                    newIds.addAll(untagged);

                    if (newIds.isEmpty()) return Collections.emptyList();
                    Collections.shuffle(newIds);
                    int take = Math.min(newIds.size(), sessionSize);
                    return newIds.subList(0, take).stream()
                            .map(id -> new QuizItem(itemType, id))
                            .collect(Collectors.toList());
                });
    }

    private Mono<List<QuizItem>> generateLearningOnly(
            UUID userId, ItemType itemType, int sessionSize,
            List<String> tags, Map<String, List<UUID>> tagToIds,
            QuizGeneratorConfig.BucketParams bucketParams,
            QuizGeneratorConfig.DueSortParams dueSortParams) {

        if (tags.isEmpty()) return Mono.just(Collections.emptyList());

        return quizItemScoreRepository
                .findLearningItems(userId, itemType, tags, bucketParams.getMasteredLowerThreshold())
                .collectList()
                .map(scores -> {
                    List<QuizItem> result = flattenScoresToItems(scores, tagToIds, sessionSize, itemType, dueSortParams);
                    return result.isEmpty() ? Collections.emptyList() : result;
                });
    }

    private Mono<List<QuizItem>> generateReviewOnly(
            UUID userId, ItemType itemType, int sessionSize,
            List<String> tags, Map<String, List<UUID>> tagToIds,
            QuizGeneratorConfig.BucketParams bucketParams,
            QuizGeneratorConfig.DueSortParams dueSortParams) {

        if (tags.isEmpty()) return Mono.just(Collections.emptyList());

        return quizItemScoreRepository
                .findReviewItems(userId, itemType, tags,
                        bucketParams.getMasteredLowerThreshold(), Instant.now())
                .collectList()
                .map(scores -> {
                    List<QuizItem> result = flattenScoresToItems(scores, tagToIds, sessionSize, itemType, dueSortParams);
                    return result.isEmpty() ? Collections.emptyList() : result;
                });
    }

    private List<QuizItem> flattenScoresToItems(
            List<QuizItemScore> scores, Map<String, List<UUID>> tagToIds,
            int sessionSize, ItemType itemType,
            QuizGeneratorConfig.DueSortParams dueSortParams) {

        if (scores.isEmpty()) return Collections.emptyList();

        List<QuizItemScore> sorted = DueItemPriorityComparator.sortByPriority(scores, dueSortParams, Instant.now());
        List<QuizItem> result = new ArrayList<>();
        for (QuizItemScore score : sorted) {
            List<UUID> ids = tagToIds.get(score.getProgressTag());
            if (ids != null) {
                for (UUID id : ids) {
                    if (result.size() >= sessionSize) break;
                    result.add(new QuizItem(itemType, id));
                }
            }
            if (result.size() >= sessionSize) break;
        }
        return result;
    }

    private List<QuizItem> takeRandom(List<QuestPoolItemDto> pool, int sessionSize, ItemType itemType) {
        List<QuestPoolItemDto> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        int take = Math.min(shuffled.size(), sessionSize);
        List<QuizItem> result = new ArrayList<>();
        for (int i = 0; i < take; i++) {
            result.add(new QuizItem(itemType, shuffled.get(i).id()));
        }
        return result;
    }
}