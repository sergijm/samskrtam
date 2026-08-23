package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.ConjugationParadigmDto;
import sm.selflearn.samskrtam.content.dto.ConjugationParadigmPageDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.dto.Difficulty;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * v2 lesson endpoints that serve the lesson picker and the lesson sub-views
 * (list / by-category / by-slug summary / vocabulary / paradigms / histories).
 */
@Service
@RequiredArgsConstructor
public class LessonV2Service {

    private final CurriculumClient curriculumClient;
    private final QuizItemScoreRepository quizItemScoreRepository;
    private final WordStatusResolver wordStatusResolver;

    /* ─── list / summary ─────────────────────────────────────────────── */

    public Mono<LessonListResponse> listAll() {
        return curriculumClient.fetchTopics(null, null)
                .map(topics -> new LessonListResponse(
                        topics.stream().map(this::toLessonItem).toList()));
    }

    public Mono<LessonListResponse> listByCategory(String category) {
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "grammar", "declensions", "declension", "conjugations", "conjugation" ->
                    curriculumClient.fetchTopicsByDomainType("GRAMMAR")
                            .map(topics -> new LessonListResponse(
                                    topics.stream().map(this::toLessonItem).toList()));
            case "lexicon", "vocabulary", "vocabulary-basic", "vocabulary-texts" ->
                    curriculumClient.fetchTopicsByDomainType("LEXICON")
                            .map(topics -> new LessonListResponse(
                                    topics.stream().map(this::toLessonItem).toList()));
            default -> Mono.just(new LessonListResponse(List.of()));
        };
    }

    public Mono<LessonItemDto> lessonBySlug(String slug) {
        return curriculumClient.fetchTopics(null, null)
                .map(list -> list.stream()
                        .filter(t -> t.code().equalsIgnoreCase(slug))
                        .findFirst()
                        .map(this::toLessonItem)
                        .orElse(null));
    }

    /* ─── vocabulary ─────────────────────────────────────────────────── */

    public Mono<VocabularyLessonDto> vocabularyLesson(String slug, UUID userId) {
        return curriculumClient.fetchTopics(null, null)
                .flatMap(topics -> topics.stream()
                        .filter(t -> t.code().equalsIgnoreCase(slug))
                        .findFirst()
                        .map(t -> buildVocabularyLesson(t, userId))
                        .orElseGet(() -> Mono.just(emptyVocabularyLesson(slug))));
    }

    private Mono<VocabularyLessonDto> buildVocabularyLesson(TopicDto topic, UUID userId) {
        return curriculumClient.fetchQuestItemsByTopic(topic.id(), "VOCABULARY_WORD")
                .map(items -> {
                    items.sort(Comparator.comparingInt(
                            it -> extractInt(it.payload(), "order", 0)));
                    return items;
                })
                .flatMap(items -> {

            VocabularyLessonDto dto = new VocabularyLessonDto();
            dto.setLessonId(topic.id());
            dto.setSlug(topic.code());
            dto.setTitleRu(topic.titleRu());
            dto.setTitleEn(topic.titleEn());
            dto.setDifficulty(mapDifficulty(topic.learningLevel()).name());
            dto.setTotalWords(items.size());

            if (userId == null || items.isEmpty()) {
                dto.setLearnedWords(0);
                dto.setProgressPercent(0f);
                dto.setWords(items.stream()
                        .map(this::emptyWordProgress)
                        .collect(Collectors.toList()));
                dto.setStatusSummary(new LessonStatusSummary(items.size(), items.size(), 0, 0, 0));
                return Mono.just(dto);
            }

            List<String> progressTags = items.stream()
                    .map(it -> it.progressTag() != null ? it.progressTag() : it.correctAnswer())
                    .filter(tag -> tag != null && !tag.isBlank())
                    .toList();

            if (progressTags.isEmpty()) {
                dto.setLearnedWords(0);
                dto.setProgressPercent(0f);
                dto.setWords(items.stream()
                        .map(this::emptyWordProgress)
                        .collect(Collectors.toList()));
                dto.setStatusSummary(new LessonStatusSummary(items.size(), items.size(), 0, 0, 0));
                return Mono.just(dto);
            }

            Instant now = Instant.now();
            return quizItemScoreRepository
                    .findByUserIdAndItemTypeAndProgressTagIn(userId, ItemType.VOCABULARY_WORD, progressTags)
                    .collectMap(QuizItemScore::getProgressTag, score -> score)
                    .map(scoresMap -> {
                        int newCount = 0, learning = 0, mastered = 0, reviewDue = 0;
                        List<VocabularyWordProgress> wordProgressList = new ArrayList<>();

                        for (QuestItemDto item : items) {
                            QuizItemScore itemScore = scoresMap.get(item.progressTag());
                            WordStatus status = wordStatusResolver.resolve(itemScore, now);

                            switch (status) {
                                case NEW -> newCount++;
                                case LEARNING -> learning++;
                                case MASTERED -> mastered++;
                                case REVIEW -> reviewDue++;
                            }

                            VocabularyWordProgress p = new VocabularyWordProgress();
                            p.setWordId(item.id());
                            p.setWord(extractString(item.payload(), "lemmaIast"));
                            p.setWordDevanagari(extractString(item.payload(), "lemmaDevanagari"));
                            p.setTranslationRu(item.correctAnswerRu());
                            p.setTranslationEn(item.correctAnswer());
                            p.setStatus(status);
                            p.setScore(itemScore != null ? itemScore.getScore() : 0);
                            wordProgressList.add(p);
                        }

                        dto.setWords(wordProgressList);
                        int learned = mastered + reviewDue;
                        dto.setLearnedWords(learned);
                        dto.setProgressPercent(items.isEmpty() ? 0f : (float) learned / items.size() * 100f);
                        dto.setStatusSummary(new LessonStatusSummary(items.size(), newCount, learning, mastered, reviewDue));
                        return dto;
                    });
                });
    }

    private VocabularyWordProgress emptyWordProgress(QuestItemDto item) {
        VocabularyWordProgress p = new VocabularyWordProgress();
        p.setWordId(item.id());
        p.setWord(extractString(item.payload(), "lemmaIast"));
        p.setWordDevanagari(extractString(item.payload(), "lemmaDevanagari"));
        p.setTranslationRu(item.correctAnswerRu());
        p.setTranslationEn(item.correctAnswer());
        p.setNSuccess(0);
        p.setNAll(0);
        p.setScore(0);
        p.setStatus(WordStatus.NEW);
        return p;
    }

    private String extractString(JsonNode payload, String field) {
        if (payload == null) return "";
        JsonNode node = payload.get(field);
        return node != null && !node.isNull() ? node.asText() : "";
    }

    private int extractInt(JsonNode payload, String field, int defaultValue) {
        if (payload == null) return defaultValue;
        JsonNode node = payload.get(field);
        return node != null && !node.isNull() ? node.asInt(defaultValue) : defaultValue;
    }

    private VocabularyLessonDto emptyVocabularyLesson(String slug) {
        VocabularyLessonDto dto = new VocabularyLessonDto();
        dto.setLessonId(UUID.randomUUID());
        dto.setSlug(slug);
        dto.setTitleRu("");
        dto.setTitleEn("");
        dto.setDifficulty("BEGINNER");
        dto.setTotalWords(0);
        dto.setLearnedWords(0);
        dto.setProgressPercent(0f);
        dto.setStatusSummary(new LessonStatusSummary(0, 0, 0, 0, 0));
        dto.setWords(List.of());
        return dto;
    }

    /* ─── paradigms & examples (v2 from curriculum-service) ─────────── */

    public Mono<DeclensionParadigmPageDto> paradigmPage(String slug, int index) {
        return curriculumClient.fetchParadigmPage(slug, index)
                .onErrorResume(e -> Mono.just(emptyParadigmPage(index)));
    }

    private DeclensionParadigmPageDto emptyParadigmPage(int index) {
        return new DeclensionParadigmPageDto(index, 0, emptyParadigm());
    }

    private DeclensionParadigmDto emptyParadigm() {
        return DeclensionParadigmDto.builder()
                .stemId(null)
                .stemIast(null)
                .stemDevanagari(null)
                .gender(Gender.UNSPECIFIED)
                .forms(List.of())
                .build();
    }

    public Mono<ConjugationParadigmPageDto> conjugationParadigmPage(String slug, int index, String voice) {
        return curriculumClient.fetchConjugationParadigmPage(slug, index, voice)
                .onErrorResume(e -> Mono.just(emptyConjugationParadigmPage(index)));
    }

    private ConjugationParadigmPageDto emptyConjugationParadigmPage(int index) {
        return ConjugationParadigmPageDto.builder()
                .index(index).totalCount(0)
                .paradigm(ConjugationParadigmDto.builder()
                        .lemmaIast(null).lemmaDevanagari(null).meaningRu(null)
                        .forms(List.of())
                        .build())
                .build();
    }

    /* ─── history dialogs (empty until quiz_item_score is sourced here) ─ */

    public Mono<WordAnswerHistory> wordHistory(String slug, UUID wordId, UUID userId,
                                               Pageable pageable, Locale locale) {
        return Mono.just(WordAnswerHistory.builder()
                .wordId(wordId)
                .word("")
                .lessonId(null)
                .entries(List.of())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .total(0)
                .build());
    }

    public Mono<QuestionAnswerHistory> questionHistory(String slug, String caseType, String numberType,
                                                        String gender, UUID userId, Pageable pageable,
                                                        Locale locale) {
        return Mono.just(QuestionAnswerHistory.builder()
                .questionId(null)
                .textRu("")
                .lessonId(null)
                .entries(List.of())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .total(0)
                .build());
    }

    /* ─── mapping helpers ────────────────────────────────────────────── */

    private LessonItemDto toLessonItem(TopicDto t) {
        boolean isVocabulary = "LEXICON".equals(t.domain()) || "VERSE".equals(t.domain());
        return LessonItemDto.builder()
                .id(t.id())
                .slug(t.code())
                .titleRu(t.titleRu())
                .titleEn(t.titleEn())
                .descriptionRu("")
                .descriptionEn("")
                .lessonType(isVocabulary ? LessonType.VOCABULARY : LessonType.DECLENSIONS)
                .difficulty(mapDifficulty(t.learningLevel()))
                .totalQuestions(0)
                .totalWordsOwn(isVocabulary ? 0 : 0)
                .learnedWords(0)
                .domain(t.domain())
                .domainType(t.domainType())
                .build();
    }

    private Difficulty mapDifficulty(String level) {
        if (level == null) {
            return Difficulty.BEGINNER;
        }
        return switch (level) {
            case "L0", "L1", "L2" -> Difficulty.BEGINNER;
            case "L3", "L4" -> Difficulty.INTERMEDIATE;
            default -> Difficulty.ADVANCED;
        };
    }
}