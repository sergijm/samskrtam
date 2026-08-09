package sm.selflearn.samskrtam.quiz.service;

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
import sm.selflearn.samskrtam.quiz.dto.DeclensionExamplesResponseDto;
import sm.selflearn.samskrtam.quiz.dto.LessonItemDto;
import sm.selflearn.samskrtam.quiz.dto.LessonListResponse;
import sm.selflearn.samskrtam.quiz.dto.LessonStatusSummary;
import sm.selflearn.samskrtam.quiz.dto.QuestionAnswerHistory;
import sm.selflearn.samskrtam.quiz.dto.TopicLessonSummaryDto;
import sm.selflearn.samskrtam.quiz.dto.VocabularyLessonDto;
import sm.selflearn.samskrtam.quiz.dto.WordAnswerHistory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * v2 lesson endpoints that serve the lesson picker and the lesson sub-views
 * (list / by-category / by-slug summary / vocabulary / paradigms / examples /
 * histories). The lesson base data comes from curriculum-service via
 * {@link CurriculumClient}; content-service is not used. Where curriculum has no
 * data yet (vocabulary, verse examples), a valid empty response is returned so the
 * page never 400/500s.
 */
@Service
@RequiredArgsConstructor
public class LessonV2Service {

    private final CurriculumClient curriculumClient;
    private final SangrahaClient sangrahaClient;

    /* ─── list / summary ─────────────────────────────────────────────── */

    public Mono<LessonListResponse> listAll() {
        return curriculumClient.fetchLessons()
                .map(summaries -> new LessonListResponse(
                        summaries.stream().map(this::toLessonItem).toList()));
    }

    public Mono<LessonListResponse> listByCategory(String category) {
        if (isDeclensionsCategory(category)) {
            return listAll();
        }
        return Mono.just(new LessonListResponse(List.of()));
    }

    public Mono<LessonItemDto> lessonBySlug(String slug) {
        return curriculumClient.fetchLessons()
                .map(list -> list.stream()
                        .filter(s -> s.code().equalsIgnoreCase(slug))
                        .findFirst()
                        .map(this::toLessonItem)
                        .orElse(null));
    }

    /* ─── vocabulary ─────────────────────────────────────────────────── */

    public Mono<VocabularyLessonDto> vocabularyLesson(String slug) {
        return Mono.just(emptyVocabularyLesson(slug));
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

    private LessonItemDto toLessonItem(TopicLessonSummaryDto s) {
        return LessonItemDto.builder()
                .id(s.id())
                .slug(s.code())
                .titleRu(s.titleRu())
                .titleEn(s.titleEn())
                .descriptionRu("")
                .descriptionEn("")
                .lessonType(LessonType.DECLENSIONS)
                .difficulty(mapDifficulty(s.learningLevel()))
                .totalQuestions(s.totalQuestions())
                .totalWordsOwn(0)
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

    private boolean isDeclensionsCategory(String category) {
        if (category == null) {
            return false;
        }
        return switch (category.toLowerCase(Locale.ROOT)) {
            case "declensions", "declension", "grammar" -> true;
            default -> false;
        };
    }
}