package sm.selflearn.samskrtam.quiz.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.service.LessonService;

import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
@Slf4j
public class QuizController {

    private final LessonService lessonService;

    @GetMapping("/lessons/{slug}")
    public Mono<ResponseEntity<VocabularyLesson>> getVocabularyLesson(
            @PathVariable String slug,
            @RequestHeader("X-User-ID") UUID userId) {
        return lessonService.getVocabularyLesson(slug, userId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/lessons/grammar/{type}")
    public Mono<ResponseEntity<GrammarLesson>> getGrammarLesson(
            @PathVariable String type,
            @RequestHeader("X-User-ID") UUID userId) {
        return lessonService.getGrammarLesson(LessonType.valueOf(type.toUpperCase()), userId)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/lessons/{slug}/history/words/{wordId}")
    public Mono<ResponseEntity<WordAnswerHistory>> getWordAnswerHistory(
            @PathVariable String slug,
            @PathVariable UUID wordId,
            @RequestHeader("X-User-ID") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "en") String lang) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Locale locale = Locale.forLanguageTag(lang);
        
        return lessonService.getWordAnswerHistory(slug, wordId, userId, pageable, locale)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }

    @GetMapping("/lessons/{slug}/history/questions/{questionId}")
    public Mono<ResponseEntity<QuestionAnswerHistory>> getQuestionAnswerHistory(
            @PathVariable String slug,
            @PathVariable UUID questionId,
            @RequestHeader("X-User-ID") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "en") String lang) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        Locale locale = Locale.forLanguageTag(lang);
        
        return lessonService.getQuestionAnswerHistory(slug, questionId, userId, pageable, locale)
                .map(ResponseEntity::ok)
                .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()));
    }
}