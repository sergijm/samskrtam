package sm.selflearn.samskrtam.quiz.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.quiz.dto.DeclensionExamplesResponseDto;
import sm.selflearn.samskrtam.quiz.dto.GrammarLesson;
import sm.selflearn.samskrtam.quiz.dto.LessonItemDto;
import sm.selflearn.samskrtam.quiz.dto.LessonListResponse;
import sm.selflearn.samskrtam.quiz.dto.QuestionAnswerHistory;
import sm.selflearn.samskrtam.quiz.dto.VocabularyLessonDto;
import sm.selflearn.samskrtam.quiz.dto.WordAnswerHistory;
import sm.selflearn.samskrtam.quiz.service.GrammarLessonV2Service;
import sm.selflearn.samskrtam.quiz.service.LessonV2Service;

import java.util.Locale;
import java.util.UUID;

/**
 * v2 lesson endpoints (API v2). The lesson base data comes from curriculum-service
 * (via {@link GrammarLessonV2Service}/{@link LessonV2Service}); the legacy v1
 * grammar endpoint that delegated to content-service is superseded by this one.
 */
@RestController
@RequestMapping("/api/v2/lessons")
@RequiredArgsConstructor
@Slf4j
public class LessonV2Controller {

    private final GrammarLessonV2Service grammarLessonV2Service;
    private final LessonV2Service lessonV2Service;

    @GetMapping
    public Mono<ResponseEntity<LessonListResponse>> getLessons(
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("v2 GET /lessons — X-User-Id={}", userId);
        return lessonV2Service.listAll().map(ResponseEntity::ok);
    }

    @GetMapping("/{param}")
    public Mono<ResponseEntity<?>> getLessonsByParam(
            @PathVariable("param") String param,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("v2 GET /lessons/{} — X-User-Id={}", param, userId);
        if (isCategory(param)) {
            return lessonV2Service.listByCategory(param).map(ResponseEntity::ok);
        }
        // slug → lesson summary (lesson picker "start session" flow)
        return lessonV2Service.lessonBySlug(param)
                .map(item -> item != null
                        ? ResponseEntity.ok((Object) item)
                        : ResponseEntity.notFound().build());
    }

    @GetMapping("/grammar/{topicCode}")
    public Mono<ResponseEntity<GrammarLesson>> getGrammarLesson(
            @PathVariable("topicCode") String topicCode,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("v2 GET /grammar/{} — X-User-Id={}", topicCode, userId);
        return grammarLessonV2Service.build(topicCode, userId)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/vocabulary/{slug}")
    public Mono<ResponseEntity<VocabularyLessonDto>> getVocabularyLesson(
            @PathVariable("slug") String slug,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("v2 GET /vocabulary/{} — X-User-Id={}", slug, userId);
        return lessonV2Service.vocabularyLesson(slug, userId).map(ResponseEntity::ok);
    }

    @GetMapping("/vocabulary/{slug}/words/{wordId}/history")
    public Mono<ResponseEntity<WordAnswerHistory>> getWordAnswerHistory(
            @PathVariable("slug") String slug,
            @PathVariable("wordId") UUID wordId,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "locale", defaultValue = "en") Locale locale) {
        Pageable pageable = pageRequest(page, size);
        return lessonV2Service.wordHistory(slug, wordId, userId, pageable, locale)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/grammar/{slug}/questions/history")
    public Mono<ResponseEntity<QuestionAnswerHistory>> getQuestionAnswerHistory(
            @PathVariable("slug") String slug,
            @RequestParam String caseType,
            @RequestParam String numberType,
            @RequestParam String gender,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "locale", defaultValue = "en") Locale locale) {
        Pageable pageable = pageRequest(page, size);
        return lessonV2Service.questionHistory(slug, caseType, numberType, gender, userId, pageable, locale)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/{slug}/declension-paradigms")
    public Mono<ResponseEntity<DeclensionParadigmPageDto>> getDeclensionParadigm(
            @PathVariable("slug") String slug,
            @RequestParam(defaultValue = "0") int index,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("v2 GET /{}/declension-paradigms?index={} — X-User-Id={}", slug, index, userId);
        return lessonV2Service.paradigmPage(slug, index).map(ResponseEntity::ok);
    }

    @GetMapping("/{slug}/examples")
    public Mono<ResponseEntity<DeclensionExamplesResponseDto>> getDeclensionExamples(
            @PathVariable("slug") String slug,
            @RequestHeader(value = "X-User-Id", required = false) UUID userId) {
        log.info("v2 GET /{}/examples — X-User-Id={}", slug, userId);
        return lessonV2Service.examples(slug).map(ResponseEntity::ok);
    }

    private boolean isCategory(String param) {
        if (param == null) {
            return false;
        }
        return switch (param.toLowerCase(Locale.ROOT)) {
            case "grammar", "declensions", "declension", "conjugations", "conjugation",
                 "lexicon",
                 "vocabulary", "vocabulary-basic", "vocabulary-texts" -> true;
            default -> false;
        };
    }

    private Pageable pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "answeredAt"));
    }
}