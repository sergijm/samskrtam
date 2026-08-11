package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.config.QuizGeneratorConfig;
import sm.selflearn.samskrtam.quiz.dto.QuestPoolItemDto;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.ProgressTagSetId;
import sm.selflearn.samskrtam.quiz.model.QuizItem;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
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
 *   <li>Получить список {@link QuestPoolItemDto} для scope</li>
 *   <li>Присоединить строки quiz_item_score по progress_tag; разделить на due/new/reserve</li>
 *   <li>Приоритизировать due по весовой формуле</li>
 *   <li>Отобрать new не более maxNewPerSession</li>
 *   <li>Добить reserve если нужно</li>
 *   <li>Добить остатком new сверх maxNewPerSession если нужно</li>
 *   <li>Перемешать с учётом interleaveCategories и minGap</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizGenerator {

    private final QuizItemScoreRepository quizItemScoreRepository;
    private final QuizGeneratorConfig config;
    private final QuizProgressTagSetGenerator progressTagSetGenerator;

    /**
     * Отобрать список {@link QuizItem} для сессии.
     *
     * @param userId  идентификатор пользователя
     * @param itemType тип элементов (VOCABULARY_WORD, DECLENSION_FORM)
     * @param pool    список элементов пула с id и progressTag
     * @return список QuizItem, отсортированный для показа
     */
    public Mono<List<QuizItem>> generate(
            UUID userId,
            ItemType itemType,
            List<QuestPoolItemDto> pool) {

        if (pool == null || pool.isEmpty()) {
            return Mono.just(Collections.emptyList());
        }

        // Extract unique progress tags
        List<String> tags = pool.stream()
                .map(QuestPoolItemDto::progressTag)
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .collect(Collectors.toList());

        if (tags.isEmpty()) {
            // No tags means all items are treated as new
            return Mono.just(buildAllNewSelection(itemType, pool, config.getSessionSize()));
        }

        return quizItemScoreRepository
                .findByUserIdAndItemTypeAndProgressTagIn(userId, itemType, tags)
                .collectList()
                .map(scores -> buildSelection(itemType, pool, scores));
    }

    private List<QuizItem> buildAllNewSelection(ItemType itemType, List<QuestPoolItemDto> pool,
                                                QuizGeneratorConfig.SessionSizeParams sessionSizeParams) {
        int sessionSize = sessionSizeParams.getSessionSize();
        List<QuizItem> selected = new ArrayList<>();
        List<QuestPoolItemDto> shuffled = new ArrayList<>(pool);
        Collections.shuffle(shuffled);
        int take = Math.min(shuffled.size(), sessionSize);
        for (int i = 0; i < take; i++) {
            selected.add(new QuizItem(itemType, shuffled.get(i).id()));
        }
        return selected;
    }

    /**
     * Основной алгоритм отбора (чистая логика, без БД).
     *
     * @param itemType       тип элементов
     * @param pool           список элементов пула с id и progressTag
     * @param existingScores уже имеющиеся строки quiz_item_score для этих progressTags
     * @return список QuizItem для сессии
     */
    List<QuizItem> buildSelection(
            ItemType itemType,
            List<QuestPoolItemDto> pool,
            List<QuizItemScore> existingScores) {

        QuizGeneratorConfig.SessionSizeParams sessionSizeParams = config.getSessionSize();
        QuizGeneratorConfig.DueSortParams dueSortParams = config.getDueSort();
        QuizGeneratorConfig.ReserveParams reserveParams = config.getReserve();
        QuizGeneratorConfig.GeneralParams generalParams = config.getGeneral();
        QuizGeneratorConfig.BucketParams bucketParams = config.getBuckets();

        int sessionSize = sessionSizeParams.getSessionSize();
        int maxNew = sessionSizeParams.getMaxNewPerSession();
        int dueCap = (int) Math.ceil(sessionSize * sessionSizeParams.getDueCapRatio());

        // Map progressTag → QuizItemScore
        Map<String, QuizItemScore> scoreMap = existingScores.stream()
                .collect(Collectors.toMap(QuizItemScore::getProgressTag, s -> s, (a, b) -> a));

        // Map progressTag → list of quest item ids
        Map<String, List<UUID>> tagToIds = pool.stream()
                .filter(p -> p.progressTag() != null && !p.progressTag().isBlank())
                .collect(Collectors.groupingBy(
                        QuestPoolItemDto::progressTag,
                        LinkedHashMap::new,
                        Collectors.mapping(QuestPoolItemDto::id, Collectors.toList())));

        // Items without a tag go to new pool
        List<UUID> untaggedIds = pool.stream()
                .filter(p -> p.progressTag() == null || p.progressTag().isBlank())
                .map(QuestPoolItemDto::id)
                .collect(Collectors.toList());

        Instant now = Instant.now();

        // Разделяем tags на due / new / reserve
        List<UUID> dueItems = new ArrayList<>();
        List<UUID> newItems = new ArrayList<>(untaggedIds);
        List<UUID> reserveItems = new ArrayList<>();

        for (Map.Entry<String, List<UUID>> entry : tagToIds.entrySet()) {
            String tag = entry.getKey();
            List<UUID> ids = entry.getValue();
            QuizItemScore score = scoreMap.get(tag);

            if (score == null) {
                // Нет строки = NEW
                newItems.addAll(ids);
            } else if (score.getNextReviewAt() != null
                    && !score.getNextReviewAt().isAfter(now)) {
                // nextReviewAt <= now → due
                dueItems.addAll(ids);
            } else if (ScoreCalculator.determineBucket(true, score.getScore(), bucketParams)
                    == ScoreCalculator.Bucket.MASTERED) {
                if (score.getNextReviewAt() != null) {
                    Instant cooldownEnd = score.getNextReviewAt()
                            .plusSeconds(generalParams.getMasteredCooldown() * 86400L);
                    if (!cooldownEnd.isAfter(now)) {
                        reserveItems.addAll(ids);
                    }
                }
            } else {
                reserveItems.addAll(ids);
            }
        }

        // 3. Приоритизация due
        List<QuizItem> selected = new ArrayList<>();
        // Map quest_item id → its tag's score for sorting
        Map<UUID, QuizItemScore> idScoreMap = new HashMap<>();
        for (Map.Entry<String, List<UUID>> entry : tagToIds.entrySet()) {
            QuizItemScore s = scoreMap.get(entry.getKey());
            if (s != null) {
                for (UUID id : entry.getValue()) {
                    idScoreMap.put(id, s);
                }
            }
        }

        dueItems.sort(DueItemPriorityComparator.comparingByPriority(idScoreMap, dueSortParams, now));
        int dueCount = Math.min(dueItems.size(), dueCap);
        for (int i = 0; i < dueCount; i++) {
            selected.add(new QuizItem(itemType, dueItems.get(i)));
        }

        // 4. Добавить new
        int remainingNew = Math.min(newItems.size(), maxNew);
        int roomAfterDue = sessionSize - selected.size();
        int newToTake = Math.min(remainingNew, roomAfterDue);
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

        // 5b. Добить остатком new сверх maxNewPerSession
        int roomAfterReserve = sessionSize - selected.size();
        if (roomAfterReserve > 0 && newToTake < newItems.size()) {
            int overflowToTake = Math.min(roomAfterReserve, newItems.size() - newToTake);
            for (int i = newToTake; i < newToTake + overflowToTake; i++) {
                selected.add(new QuizItem(itemType, newItems.get(i)));
            }
        }

        // 6. Перемешивание
        if (generalParams.isInterleaveCategories() && selected.size() > 1) {
            Collections.shuffle(selected);
        }

        return selected;
    }

    /**
     * Отобрать список {@link QuizItem} для сессии по именованному прогресс-сету (§4 п.«2а»).
     * Делегирует в {@link QuizProgressTagSetGenerator}.
     *
     * @return Mono со списком QuizItem; если пул сета пуст — Mono.empty() (→ SCOPE_FILTER_EMPTY)
     */
    public Mono<List<QuizItem>> generateByProgressTagSet(
            UUID userId,
            ItemType itemType,
            List<QuestPoolItemDto> pool,
            String progressTagSetId) {
        ProgressTagSetId set;
        try {
            set = ProgressTagSetId.valueOf(progressTagSetId);
        } catch (IllegalArgumentException e) {
            throw new sm.selflearn.samskrtam.common.SamskrtamException(
                    "UNKNOWN_PROGRESS_TAG_SET", "Unknown progress tag set: " + progressTagSetId);
        }
        return progressTagSetGenerator.generate(userId, itemType, pool, set);
    }
}