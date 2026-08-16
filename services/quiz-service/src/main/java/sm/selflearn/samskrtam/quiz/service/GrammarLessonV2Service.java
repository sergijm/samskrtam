package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds a grammar lesson (progress grid) for a curriculum topic (API v2).
 * Topic metadata comes from {@code fetchTopics}, quest items (for progress tag
 * morphology attributes) from {@code fetchAllQuestItems}, per-tag progress from
 * {@code quiz_item_score}.
 */
@Service
@RequiredArgsConstructor
public class GrammarLessonV2Service {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private record TagInfo(String itemType, String gender, String caseType,
                           String numberType, String formIast) {}

    private record TagProgress(String caseType, String numberType,
                               int score, WordStatus status) {}

    private final CurriculumClient curriculumClient;
    private final QuizItemScoreRepository quizItemScoreRepository;
    private final WordStatusResolver wordStatusResolver;
    private final GrammarProgressAggregationService aggregationService;

    public Mono<GrammarLesson> build(String topicCode, UUID userId) {
        return curriculumClient.fetchTopics(null, null)
                .flatMap(topics -> topics.stream()
                        .filter(t -> t.code().equalsIgnoreCase(topicCode))
                        .findFirst()
                        .map(topic -> buildFromTopic(topic, userId))
                        .orElse(Mono.empty()));
    }

    private Mono<GrammarLesson> buildFromTopic(TopicDto topic, UUID userId) {
        return curriculumClient.fetchAllQuestItems(topic.id())
                .flatMap(items -> {
                    Map<String, TagInfo> metadata = extractTags(items);
                    if (userId == null || metadata.isEmpty()) {
                        return Mono.just(emptyLesson(topic, metadata));
                    }
                    ItemType itemType = resolveItemType(metadata);
                    List<String> tags = List.copyOf(metadata.keySet());
                    return quizItemScoreRepository
                            .findByUserIdAndItemTypeAndProgressTagIn(userId, itemType, tags)
                            .collectMap(QuizItemScore::getProgressTag, score -> score)
                            .map(scoresMap -> assemble(topic, metadata, scoresMap));
                });
    }

    private GrammarLesson emptyLesson(TopicDto topic, Map<String, TagInfo> metadata) {
        List<TagProgress> progress = new ArrayList<>();
        for (var entry : metadata.entrySet()) {
            TagInfo info = entry.getValue();
            progress.add(new TagProgress(info.caseType(), info.numberType(),
                    0, WordStatus.NEW));
        }
        LessonStatusSummary summary = new LessonStatusSummary(metadata.size(),
                metadata.size(), 0, 0, 0);
        return populate(topic, progress, summary, 0, 0f);
    }

    private GrammarLesson assemble(TopicDto topic, Map<String, TagInfo> metadata,
                                    Map<String, QuizItemScore> scoresMap) {
        int newCount = 0, learning = 0, mastered = 0, reviewDue = 0;
        List<TagProgress> progress = new ArrayList<>();

        Instant now = Instant.now();
        for (var entry : metadata.entrySet()) {
            String tag = entry.getKey();
            TagInfo info = entry.getValue();
            QuizItemScore score = scoresMap.get(tag);
            WordStatus status = wordStatusResolver.resolve(score, now);

            switch (status) {
                case NEW -> newCount++;
                case LEARNING -> learning++;
                case MASTERED -> mastered++;
                case REVIEW -> reviewDue++;
            }
            progress.add(new TagProgress(info.caseType(), info.numberType(),
                    score != null ? score.getScore() : 0, status));
        }

        int total = metadata.size();
        int learned = mastered + reviewDue;
        return populate(topic, progress,
                new LessonStatusSummary(total, newCount, learning, mastered, reviewDue),
                learned, total > 0 ? (float) learned / total * 100f : 0f);
    }

    private GrammarLesson populate(TopicDto topic, List<TagProgress> progress,
                                    LessonStatusSummary summary, int learned, float pct) {
        List<GrammarProgressAggregationService.ItemAgg> items = progress.stream()
                .map(p -> new GrammarProgressAggregationService.ItemAgg(
                        p.caseType(), p.numberType(), p.score()))
                .collect(Collectors.toList());
        GrammarProgressAggregationService.GrammarProgressAggregations aggregations =
                aggregationService.aggregate(items);

        GrammarLesson l = new GrammarLesson();
        l.setLessonId(topic.id());
        l.setType("DECLENSIONS");
        l.setTitleRu(topic.titleRu());
        l.setTitleEn(topic.titleEn());
        l.setDifficulty(topic.learningLevel());
        l.setTotalQuestions(progress.size());
        l.setLearnedQuestions(learned);
        l.setProgressPercent(pct);
        l.setStatusSummary(summary);
        l.setCaseAggregations(aggregations.caseAggregations());
        l.setNumberAggregations(aggregations.numberAggregations());
        l.setGrid(aggregations.grid());
        l.setPairAggregations(aggregations.pairAggregations());
        return l;
    }

    private Map<String, TagInfo> extractTags(List<QuestItemDto> items) {
        Map<String, TagInfo> metadata = new LinkedHashMap<>();
        for (QuestItemDto qi : items) {
            if (qi.progressTag() == null) continue;
            String json = qi.payload() != null ? qi.payload().toString() : null;
            if (json == null) continue;
            String itemType = qi.itemType();
            switch (itemType) {
                case "DECLENSION_FORM", "DECLENSION_FORM_CHOICE" -> {
                    var p = parse(json, DeclFormPayload.class);
                    metadata.putIfAbsent(qi.progressTag(), new TagInfo(itemType,
                            p.gender(), p.caseType(), p.numberType(), p.correctFormIast()));
                }
                case "CASE_RECOGNITION" -> {
                    var p = parse(json, CaseRecogPayload.class);
                    metadata.putIfAbsent(qi.progressTag(), new TagInfo(itemType,
                            p.correctGender(), p.correctCaseType(), p.correctNumberType(), p.wordFormIast()));
                }
                case "DECLENSION_MATCH" -> {
                    var p = parse(json, DeclMatchPayload.class);
                    var first = p.pairs() == null || p.pairs().isEmpty() ? null : p.pairs().get(0);
                    if (first != null) {
                        metadata.putIfAbsent(qi.progressTag(), new TagInfo(itemType,
                                null, first.caseType(), first.numberType(), null));
                    }
                }
                case "CASE_MEANING" -> {
                    var p = parse(json, CaseMeanPayload.class);
                    if (p.caseType() != null) {
                        metadata.putIfAbsent(qi.progressTag(), new TagInfo(itemType,
                                null, p.caseType().toUpperCase(), null, null));
                    }
                }
            }
        }
        return metadata;
    }

    private ItemType resolveItemType(Map<String, TagInfo> metadata) {
        for (TagInfo info : metadata.values()) {
            if (info.itemType() != null) {
                if (info.itemType().startsWith("DECLENSION_") || info.itemType().startsWith("CASE_")) {
                    return ItemType.DECLENSION_FORM;
                }
                return ItemType.VOCABULARY_WORD;
            }
        }
        return ItemType.DECLENSION_FORM;
    }

    private <T> T parse(String json, Class<T> type) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse payload as " + type.getSimpleName(), e);
        }
    }

    private record DeclFormPayload(String gender, String caseType, String numberType,
                                    String correctFormIast) {}
    private record CaseRecogPayload(String correctGender, String correctCaseType,
                                     String correctNumberType, String wordFormIast) {}
    private record DeclMatchPayload(List<MatchPair> pairs) {
        record MatchPair(String caseType, String numberType) {}
    }
    private record CaseMeanPayload(String caseType) {}
}