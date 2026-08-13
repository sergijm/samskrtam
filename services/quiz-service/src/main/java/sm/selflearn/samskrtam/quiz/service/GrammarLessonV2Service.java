package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.localization.CaseNumberGenderLocalizer;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    private final CurriculumClient curriculumClient;
    private final QuizItemScoreRepository quizItemScoreRepository;
    private final WordStatusResolver wordStatusResolver;

    public Mono<GrammarLesson> build(String topicCode, UUID userId) {
        return curriculumClient.fetchTopics(null)
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
                        return Mono.just(emptyLesson(topic, metadata.size(), metadata));
                    }
                    ItemType itemType = resolveItemType(metadata);
                    List<String> tags = List.copyOf(metadata.keySet());
                    return quizItemScoreRepository
                            .findByUserIdAndItemTypeAndProgressTagIn(userId, itemType, tags)
                            .collectMap(QuizItemScore::getProgressTag, score -> score)
                            .map(scoresMap -> assemble(topic, metadata, scoresMap));
                });
    }

    private GrammarLesson emptyLesson(TopicDto topic, int total, Map<String, TagInfo> metadata) {
        List<GrammarQuestionProgress> items = new ArrayList<>();
        for (var entry : metadata.entrySet()) {
            items.add(toProgress(entry.getKey(), entry.getValue(), null, WordStatus.NEW));
        }
        return populate(topic, total, items, new LessonStatusSummary(total, total, 0, 0, 0), 0, 0f);
    }

    private GrammarLesson assemble(TopicDto topic, Map<String, TagInfo> metadata,
                                    Map<String, QuizItemScore> scoresMap) {
        int newCount = 0, learning = 0, mastered = 0, reviewDue = 0;
        List<GrammarQuestionProgress> items = new ArrayList<>();

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
            items.add(toProgress(tag, info, score, status));
        }

        int total = metadata.size();
        int learned = mastered + reviewDue;
        return populate(topic, total, items,
                new LessonStatusSummary(total, newCount, learning, mastered, reviewDue),
                learned, total > 0 ? (float) learned / total * 100f : 0f);
    }

    private GrammarLesson populate(TopicDto topic, int total, List<GrammarQuestionProgress> items,
                                    LessonStatusSummary summary, int learned, float pct) {
        GrammarLesson l = new GrammarLesson();
        l.setLessonId(topic.id());
        l.setType("DECLENSIONS");
        l.setTitleRu(topic.titleRu());
        l.setTitleEn(topic.titleEn());
        l.setDifficulty(topic.learningLevel());
        l.setTotalQuestions(total);
        l.setLearnedQuestions(learned);
        l.setProgressPercent(pct);
        l.setStatusSummary(summary);
        l.setItems(items);
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

    private GrammarQuestionProgress toProgress(String tag, TagInfo info,
                                                QuizItemScore score, WordStatus status) {
        String caseType = info.caseType();
        String numberType = info.numberType();
        String gender = info.gender() != null ? info.gender() : "UNSPECIFIED";

        GrammarQuestionProgress p = new GrammarQuestionProgress();
        p.setQuestionId(UUID.nameUUIDFromBytes(tag.getBytes()));
        p.setTextRu(contentLabel("ru", caseType, numberType, info));
        p.setTextEn(contentLabel("en", caseType, numberType, info));
        p.setCaseType(caseType);
        p.setCaseRu(CaseNumberGenderLocalizer.caseTypeRu(caseType));
        p.setCaseEn(CaseNumberGenderLocalizer.caseTypeEn(caseType));
        p.setNumberType(numberType);
        p.setNumberRu(CaseNumberGenderLocalizer.numberTypeRu(numberType));
        p.setNumberEn(CaseNumberGenderLocalizer.numberTypeEn(numberType));
        p.setGender(gender);
        p.setGenderRu(CaseNumberGenderLocalizer.genderRu(gender));
        p.setGenderEn(CaseNumberGenderLocalizer.genderEn(gender));
        p.setScore(score != null ? score.getScore() : 0);
        p.setStatus(status);
        return p;
    }

    private String contentLabel(String lang, String caseType, String numberType, TagInfo info) {
        String caseName = "ru".equals(lang)
                ? CaseNumberGenderLocalizer.caseTypeRu(caseType)
                : CaseNumberGenderLocalizer.caseTypeEn(caseType);
        String numberName = "ru".equals(lang)
                ? CaseNumberGenderLocalizer.numberTypeRu(numberType)
                : CaseNumberGenderLocalizer.numberTypeEn(numberType);
        if (caseName != null || numberName != null) {
            return (caseName != null ? caseName : "") + ", " + (numberName != null ? numberName : "");
        }
        return info.formIast() != null ? info.formIast() : "?";
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
}