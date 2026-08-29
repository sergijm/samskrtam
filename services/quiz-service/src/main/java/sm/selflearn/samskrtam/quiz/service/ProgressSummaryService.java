package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.BulkProgressRequest;
import sm.selflearn.samskrtam.quiz.dto.BulkProgressResponse;
import sm.selflearn.samskrtam.quiz.dto.ProgressSummaryDto;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Агрегация «реального» прогресса пользователя. Единственный источник правды
 * о прогрессе — таблица {@code quiz.quiz_item_score}; все вычисления идут через
 * quiz-service (curriculum-service / фронт обращаются сюда, а не считают сами).
 */
@Service
@RequiredArgsConstructor
public class ProgressSummaryService {

    private final QuizItemScoreRepository quizItemScoreRepository;

    private static final List<ItemType> ALL_ITEM_TYPES = List.of(
            ItemType.DECLENSION_FORM, ItemType.CONJUGATION_FORM, ItemType.VOCABULARY_WORD);

    private static final Map<String, List<ItemType>> SCOPE_ITEM_TYPES = Map.of(
            "learn-graph", ALL_ITEM_TYPES,
            "grammar", List.of(ItemType.DECLENSION_FORM, ItemType.CONJUGATION_FORM),
            "lexicon", List.of(ItemType.VOCABULARY_WORD));

    /**
     * Сводка прогресса по области (scope). Без.userId — пустая сводка.
     */
    public Mono<ProgressSummaryDto> summarize(String rawScope, UUID userId) {
        String scope = rawScope == null ? "learn-graph" : rawScope.toLowerCase(Locale.ROOT);
        List<ItemType> itemTypes = SCOPE_ITEM_TYPES.getOrDefault(scope, ALL_ITEM_TYPES);
        if (userId == null) {
            return Mono.just(new ProgressSummaryDto(scope, 0, 0, 0, 0));
        }
        return quizItemScoreRepository.findByUserIdAndItemTypeIn(userId, itemTypes)
                .collectList()
                .map(scores -> aggregate(scope, scores));
    }

    /**
     * Пакетное получение оценок прогресса по конкретному itemType и списку тегов.
     */
    public Mono<BulkProgressResponse> bulkScores(UUID userId, ItemType itemType, List<String> progressTags) {
        String itemTypeName = itemType == null ? "" : itemType.name();
        if (userId == null || itemType == null || progressTags == null || progressTags.isEmpty()) {
            return Mono.just(new BulkProgressResponse(itemTypeName, Map.of()));
        }
        return quizItemScoreRepository
                .findByUserIdAndItemTypeAndProgressTagIn(userId, itemType, progressTags)
                .collectMap(QuizItemScore::getProgressTag, QuizItemScore::getScore)
                .map(scores -> new BulkProgressResponse(itemTypeName, scores));
    }

    private ProgressSummaryDto aggregate(String scope, List<QuizItemScore> scores) {
        if (scores.isEmpty()) {
            return new ProgressSummaryDto(scope, 0, 0, 0, 0);
        }
        int total = scores.size();
        int mastered = 0;
        int learned = 0;
        int sum = 0;
        for (QuizItemScore s : scores) {
            int sc = s.getScore();
            sum += sc;
            if (sc >= ProgressConstants.MASTERED_LOWER_THRESHOLD) {
                mastered++;
            } else if (sc > 0) {
                learned++;
            }
        }
        int percent = (int) Math.round((double) sum / total);
        return new ProgressSummaryDto(scope, total, mastered, learned, percent);
    }

    public static ItemType parseItemType(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return ItemType.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
