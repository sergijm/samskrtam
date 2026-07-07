package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.quiz.dto.AnswerHistoryEntry;
import sm.selflearn.samskrtam.quiz.dto.LessonItemDto;
import sm.selflearn.samskrtam.quiz.dto.LessonListResponse;
import sm.selflearn.samskrtam.quiz.dto.QuestionAnswerHistory;
import sm.selflearn.samskrtam.quiz.dto.VocabularyLessonDto;
import sm.selflearn.samskrtam.quiz.dto.WordAnswerHistory;
import sm.selflearn.samskrtam.quiz.dto.GrammarLesson;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerHistoryProjection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final ContentClient contentClient;
        private final UserSessionService userSessionService;
        private final QuizAnswerRepository quizAnswerRepository;

    private final VocabularyProgressService vocabularyProgressService;
    private final GrammarProgressService grammarProgressService;

    public Mono<VocabularyLessonDto> getVocabularyLesson(String slug, UUID userId) {
        return contentClient.getLessonItemBySlug(slug)
                .flatMap(lessonSummary ->
                        contentClient.getVocabularyWordsForLesson(lessonSummary.getId(), 100000)
                                .flatMap(vocabularyWords ->
                                        vocabularyProgressService.createVocabularyLesson(
                                                lessonSummary, vocabularyWords, userId)
                                ));
    }

            public Mono<GrammarLesson> getGrammarLesson(String slug, UUID userId) {
        return grammarProgressService.getGrammarLesson(slug, userId);
    }

    public Mono<WordAnswerHistory> getWordAnswerHistory(
            String slug, UUID wordId, UUID userId, Pageable pageable, Locale locale) {
        return contentClient.getLessonItemBySlug(slug)
                .flatMap(lessonSummary ->
                        contentClient.getVocabularyWordById(wordId)
                                .flatMap(vocabularyWord ->
                                        vocabularyProgressService.createWordAnswerHistory(
                                                wordId, lessonSummary.getId(),
                                                vocabularyWord.getWordIast(),
                                                userId, pageable)))
                .switchIfEmpty(Mono.empty());
    }

    public Mono<LessonListResponse> getLessonsByType(String lessonType, UUID userId) {
        return contentClient.getQuizzesByCategory(lessonType)
                .flatMap(lessons -> Flux.fromIterable(lessons)
                        .flatMap(lesson -> enrichLessonWithProgress(lesson, userId))
                .collectList()
                        .map(LessonListResponse::new));
    }

    public Mono<QuestionAnswerHistory> getQuestionAnswerHistory(
            String slug, String caseType, String numberType, String gender, UUID userId, Pageable pageable, Locale locale) {
        return contentClient.getLessonItemBySlug(slug)
                .flatMap(lessonSummary ->
                        quizAnswerRepository.findGrammarHistory(
                                        caseType, numberType, gender, userId, lessonSummary.getId(),
                                        pageable.getPageSize(), pageable.getOffset())
                                .collectList()
                                .flatMap(answers ->
                                        quizAnswerRepository.countGrammarHistory(caseType, numberType, gender, userId, lessonSummary.getId())
                                                .map(total -> {
                                                    List<AnswerHistoryEntry> entries = answers.stream()
                                                            .map(a -> AnswerHistoryEntry.builder()
                                                                .answeredAt(a.getAnsweredAt())
                                                                    .correctAnswer(a.getCorrectFormIast())
                                                                    .userAnswer(a.getSelectedAnswer())
                                                                    .correct(a.getIsCorrect() != null && a.getIsCorrect())
                                                                    .build())
                                                            .collect(Collectors.toList());
                                                    return QuestionAnswerHistory.builder()
                                                        .questionId(null)
                                                            .textRu(caseType + ", " + numberType)
                                                            .lessonId(lessonSummary.getId())
                                                            .entries(entries)
                                                            .page(pageable.getPageNumber())
                                                            .size(pageable.getPageSize())
                                                            .total(total.intValue())
                                                            .build();
                                                })
                                )
                )
                .switchIfEmpty(Mono.just(
                        QuestionAnswerHistory.builder()
                                .questionId(null)
                                .textRu(caseType + ", " + numberType)
                                .lessonId(null)
                                .entries(Collections.emptyList())
                                .page(pageable.getPageNumber())
                                .size(pageable.getPageSize())
                                .total(0)
                                .build()
                ));
    }

    private Mono<LessonItemDto> enrichLessonWithProgress(LessonItemResponse lesson, UUID userId) {
                                if (userId != null && LessonType.isVocabulary(lesson.getLessonType())) {
            return vocabularyProgressService.enrichWithProgress(lesson, userId);
        }
        if (userId != null && !LessonType.isVocabulary(lesson.getLessonType())) {
            return grammarProgressService.enrichWithProgress(lesson, userId);
        }
        return Mono.just(LessonItemDto.builder()
                .id(lesson.getId())
                .slug(lesson.getSlug())
                .titleRu(lesson.getTitleRu())
                .titleEn(lesson.getTitleEn())
                .descriptionRu(lesson.getDescriptionRu())
                .descriptionEn(lesson.getDescriptionEn())
                .lessonType(lesson.getLessonType())
                .difficulty(lesson.getDifficulty())
                .totalQuestions(lesson.getTotalQuestions())
                .totalWordsOwn(0)
                .learnedWords(0)
                .build());
    }
}

