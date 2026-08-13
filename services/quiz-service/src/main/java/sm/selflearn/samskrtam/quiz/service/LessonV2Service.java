package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmDto;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.dto.Difficulty;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * v2 lesson endpoints that serve the lesson picker and the lesson sub-views
 * (list / by-category / by-slug summary / vocabulary / paradigms / examples /
 * histories).
 */
@Service
@RequiredArgsConstructor
public class LessonV2Service {

    private final CurriculumClient curriculumClient;
    private final SangrahaClient sangrahaClient;
    private final QuizItemScoreRepository quizItemScoreRepository;
    private final WordStatusResolver wordStatusResolver;

    /* ─── list / summary ─────────────────────────────────────────────── */

    public Mono<LessonListResponse> listAll() {
        return curriculumClient.fetchTopics(null)
                .map(topics -> new LessonListResponse(
                        topics.stream().map(this::toLessonItem).toList()));
    }

    public Mono<LessonListResponse> listByCategory(String category) {
        String domain = switch (category.toLowerCase(Locale.ROOT)) {
            case "declensions", "declension", "grammar" -> "GRAMMAR";
            case "vocabulary", "vocabulary-basic", "vocabulary-texts" -> "LEXICON";
            case "conjugations", "conjugation" -> "GRAMMAR";
            default -> null;
        };
        if (domain == null) {
            return Mono.just(new LessonListResponse(List.of()));
        }
        return curriculumClient.fetchTopics(domain)
                .map(topics -> new LessonListResponse(
                        topics.stream().map(this::toLessonItem).toList()));
    }

    public Mono<LessonItemDto> lessonBySlug(String slug) {
        return curriculumClient.fetchTopics(null)
                .map(list -> list.stream()
                        .filter(t -> t.code().equalsIgnoreCase(slug))
                        .findFirst()
                        .map(this::toLessonItem)
                        .orElse(null));
    }

    /* ─── vocabulary ─────────────────────────────────────────────────── */

    public Mono<VocabularyLessonDto> vocabularyLesson(String slug, UUID userId) {
        return curriculumClient.fetchTopics(null)
                .flatMap(topics -> topics.stream()
                        .filter(t -> t.code().equalsIgnoreCase(slug))
                        .findFirst()
                        .map(t -> buildVocabularyLesson(t, userId))
                        .orElseGet(() -> Mono.just(emptyVocabularyLesson(slug))));
    }

    private Mono<VocabularyLessonDto> buildVocabularyLesson(TopicDto topic, UUID userId) {
        return curriculumClient.fetchQuestItemsByTopic(topic.id(), "VOCABULARY_WORD")
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

    public Mono<DeclensionExamplesResponseDto> examples(String slug) {
        return curriculumClient.fetchParadigmPage(slug, 0)
                .flatMap(page -> {
                    DeclensionParadigmDto dto = page.getParadigm();
                    if (dto == null || dto.getVowelType() == null) {
                        return Mono.just(new DeclensionExamplesResponseDto(List.of()));
                    }
                    String vowelType = dto.getVowelType().name();
                    String gender = dto.getGender() != null ? dto.getGender().name() : "UNSPECIFIED";

                    List<Map<String, String>> cells = new ArrayList<>();
                    for (CaseType c : CaseType.values()) {
                        for (NumberType n : new NumberType[]{NumberType.SINGULAR, NumberType.DUAL, NumberType.PLURAL}) {
                            cells.add(Map.of("caseType", c.name(), "numberType", n.name()));
                        }
                    }

                    return sangrahaClient.searchExamples(vowelType, gender, 5, 10, cells)
                            .flatMap(searchResp -> {
                                List<UUID> allVerseIds = searchResp.groups().stream()
                                        .flatMap(g -> g.verseIds().stream())
                                        .distinct()
                                        .collect(Collectors.toList());
                                if (allVerseIds.isEmpty()) {
                                    return Mono.just(new DeclensionExamplesResponseDto(List.of()));
                                }
                                return sangrahaClient.fetchVersesBatch(allVerseIds)
                                        .map(batchResp -> {
                                            Map<UUID, SangrahaVersesBatchResponse.VerseDto> verseMap =
                                                    batchResp.verses().stream()
                                                            .collect(Collectors.toMap(
                                                                    SangrahaVersesBatchResponse.VerseDto::verseId,
                                                                    v -> v,
                                                                    (a, b) -> a));

                                            List<DeclensionExamplesResponseDto.GroupDto> groups = new ArrayList<>();
                                            for (SangrahaExamplesSearchResponse.GroupDto group : searchResp.groups()) {
                                                if (group.verseIds().isEmpty()) continue;
                                                List<DeclensionExamplesResponseDto.ExampleDto> examples =
                                                        group.verseIds().stream()
                                                                .map(verseMap::get)
                                                                .filter(v -> v != null)
                                                                .map(v -> DeclensionExamplesResponseDto.ExampleDto.builder()
                                                                        .verseId(v.verseId())
                                                                        .workSlug(v.workSlug())
                                                                        .textIast(v.textIast())
                                                                        .textDevanagari(v.textDevanagari())
                                                                        .translationRu(v.translationRu())
                                                                        .translationEn(v.translationEn())
                                                                        .workTitleRu(v.workTitleRu())
                                                                        .workTitleEn(v.workTitleEn())
                                                                        .chapterTitleRu(v.chapterTitleRu())
                                                                        .chapterTitleEn(v.chapterTitleEn())
                                                                        .verseOrderIndex(v.verseOrderIndex())
                                                                        .build())
                                                                .collect(Collectors.toList());
                                                groups.add(DeclensionExamplesResponseDto.GroupDto.builder()
                                                        .caseType(group.caseType())
                                                        .numberType(group.numberType())
                                                        .examples(examples)
                                                        .build());
                                            }
                                            return new DeclensionExamplesResponseDto(groups);
                                        });
                            });
                })
                .onErrorResume(e -> Mono.just(new DeclensionExamplesResponseDto(List.of())));
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
        boolean isVocabulary = "LEXICON".equals(t.domain());
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