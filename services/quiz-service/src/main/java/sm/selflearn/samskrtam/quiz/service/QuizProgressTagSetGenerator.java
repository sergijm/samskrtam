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
 * Генератор вопросов по именованному прогресс-сету (ProgressTagSetId) — §3, §4 п.«2а».
 * Отбирает элементы только из указанного среза, не смешивая due/new/reserve:
 * весь срез (все progressTag сета) без ограничений maxNewPerSession/dueCapRatio.
 *
 * <p>Статусные сеты (NEW/LEARNING/MASTERED/DIFFICULT) — по правилам quest-engine.md §2.4;
 * грамматические (SINGULAR/DUAL/PLURAL, ACC_LOC/INS_ABL/GEN_LOC/DAT_ACC) — по атрибутам
 * progressTag в пределах scope, все статусы внутри среза.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuizProgressTagSetGenerator {

    private final QuizItemScoreRepository quizItemScoreRepository;
    private final QuizGeneratorConfig config;

    /**
     * Отобрать список {@link QuizItem} для сессии по прогресс-сету.
     *
     * @return Mono со списком QuizItem; если пул сета пуст — Mono.empty() (→ SCOPE_FILTER_EMPTY)
     */
    public Mono<List<QuizItem>> generate(
            UUID userId,
            ItemType itemType,
            List<QuestPoolItemDto> pool,
            ProgressTagSetId progressTagSetId) {

        if (pool == null || pool.isEmpty() || progressTagSetId == null) {
            return Mono.empty();
        }

        int sessionSize = config.getSessionSize().getSessionSize();

        // Group pool items by progressTag for lookup
        Map<String, List<UUID>> tagToIds = pool.stream()
                .filter(p -> p.progressTag() != null && !p.progressTag().isBlank())
                .collect(Collectors.groupingBy(
                        QuestPoolItemDto::progressTag,
                        LinkedHashMap::new,
                        Collectors.mapping(QuestPoolItemDto::id, Collectors.toList())));

        List<String> tags = new ArrayList<>(tagToIds.keySet());

        return switch (progressTagSetId) {
            case NEW -> generateNewOnly(userId, itemType, pool, sessionSize, tags, tagToIds);
            case LEARNING -> generateLearningOnly(userId, itemType, sessionSize, tags, tagToIds);
            case MASTERED -> generateMasteredOnly(userId, itemType, sessionSize, tags, tagToIds);
            case DIFFICULT -> generateDifficultOnly(userId, itemType, sessionSize, tags, tagToIds);
            case SINGULAR, DUAL, PLURAL, ACC_LOC, INS_ABL, GEN_LOC, DAT_ACC ->
                    generateByGrammarSet(pool, itemType, progressTagSetId, sessionSize);
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
            List<String> tags, Map<String, List<UUID>> tagToIds) {

        if (tags.isEmpty()) return Mono.just(Collections.emptyList());

        return quizItemScoreRepository
                .findLearningItems(userId, itemType, tags, config.getBuckets().getMasteredLowerThreshold())
                .collectList()
                .map(scores -> flattenScoresToItems(scores, tagToIds, sessionSize, itemType));
    }

    private Mono<List<QuizItem>> generateMasteredOnly(
            UUID userId, ItemType itemType, int sessionSize,
            List<String> tags, Map<String, List<UUID>> tagToIds) {

        if (tags.isEmpty()) return Mono.just(Collections.emptyList());

        return quizItemScoreRepository
                .findReviewItems(userId, itemType, tags,
                        config.getBuckets().getMasteredLowerThreshold(), Instant.now())
                .collectList()
                .map(scores -> flattenScoresToItems(scores, tagToIds, sessionSize, itemType));
    }

    private Mono<List<QuizItem>> generateDifficultOnly(
            UUID userId, ItemType itemType, int sessionSize,
            List<String> tags, Map<String, List<UUID>> tagToIds) {

        if (tags.isEmpty()) return Mono.just(Collections.emptyList());

        return quizItemScoreRepository
                .findDifficultItems(userId, itemType, tags,
                        config.getBuckets().getDifficultUpperThreshold(),
                        config.getBuckets().getDifficultExitMargin())
                .collectList()
                .map(scores -> flattenScoresToItems(scores, tagToIds, sessionSize, itemType));
    }

    /**
     * Грамматические сеты (SINGULAR/DUAL/PLURAL, ACC_LOC/INS_ABL/GEN_LOC/DAT_ACC):
     * фильтр по атрибутам progressTag ("caseType|numberType|gender" для склонений),
     * все статусы внутри среза, без ограничений по размеру.
     */
    private Mono<List<QuizItem>> generateByGrammarSet(
            List<QuestPoolItemDto> pool, ItemType itemType,
            ProgressTagSetId set, int sessionSize) {

        List<QuizItem> selected = pool.stream()
                .filter(p -> p.progressTag() != null && !p.progressTag().isBlank())
                .filter(p -> matchesGrammarSet(p.progressTag(), set))
                .map(p -> new QuizItem(itemType, p.id()))
                .collect(Collectors.toList());

        if (selected.isEmpty()) return Mono.just(Collections.emptyList());
        Collections.shuffle(selected);
        int take = Math.min(selected.size(), sessionSize);
        return Mono.just(selected.subList(0, take));
    }

    private boolean matchesGrammarSet(String progressTag, ProgressTagSetId set) {
        String[] parts = progressTag.split("\\|");
        String numberType = parts.length > 1 ? parts[1] : null;
        String caseType = parts.length > 0 ? parts[0] : null;

        switch (set) {
            case SINGULAR: return "SINGULAR".equals(numberType);
            case DUAL: return "DUAL".equals(numberType);
            case PLURAL: return "PLURAL".equals(numberType);
            case ACC_LOC: return "ACCUSATIVE".equals(caseType) || "LOCATIVE".equals(caseType);
            case INS_ABL: return "INSTRUMENTAL".equals(caseType) || "ABLATIVE".equals(caseType);
            case GEN_LOC: return "GENITIVE".equals(caseType) || "LOCATIVE".equals(caseType);
            case DAT_ACC: return "DATIVE".equals(caseType) || "ACCUSATIVE".equals(caseType);
            default: return false;
        }
    }

    private List<QuizItem> flattenScoresToItems(
            List<QuizItemScore> scores, Map<String, List<UUID>> tagToIds,
            int sessionSize, ItemType itemType) {

        if (scores.isEmpty()) return Collections.emptyList();

        List<QuizItemScore> sorted = DueItemPriorityComparator.sortByPriority(
                scores, config.getDueSort(), Instant.now());
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
