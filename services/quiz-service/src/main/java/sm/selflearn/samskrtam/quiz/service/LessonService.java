package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.QuizAnswer;
import sm.selflearn.samskrtam.quiz.repository.QuizAnswerRepository;
import sm.selflearn.samskrtam.quiz.repository.WordScoreRepository;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final ContentClient contentClient;
    private final QuizDataAssembler quizDataAssembler;
    private final UserSessionService userSessionService;
    private final QuizAnswerRepository quizAnswerRepository;
    private final WordScoreRepository wordScoreRepository;

    public Mono<VocabularyLessonDto> getVocabularyLesson(String slug, UUID userId) {
        return contentClient.getLessonItemBySlug(slug)
                .flatMap(lessonSummary ->
                        contentClient.getVocabularyWordsForLesson(lessonSummary.getId(), 100000)
                                .flatMap(vocabularyWords ->
                                        createVocabularyLesson(lessonSummary, vocabularyWords, userId)
                                ));
    }

    public Mono<GrammarLesson> getGrammarLesson(LessonType type, UUID userId) {
        return Mono.empty();
    }

    public Mono<WordAnswerHistory> getWordAnswerHistory(String slug, UUID wordId, UUID userId, Pageable pageable, Locale locale) {
        return contentClient.getLessonItemBySlug(slug)
                .flatMap(lessonSummary ->
                        contentClient.getVocabularyWordById(wordId)
                                .flatMap(vocabularyWord -> createWordAnswerHistory(
                                        wordId, lessonSummary.getId(),
                                        vocabularyWord.getWordIast(),
                                        userId, pageable, locale)))
                .switchIfEmpty(Mono.empty());
    }

    public Mono<LessonListResponse> getLessonsByType(String lessonType, UUID userId) {
        return contentClient.getQuizzesByCategory(lessonType)
                .flatMap(lessons -> Flux.fromIterable(lessons)
                        .flatMap(lesson -> enrichLessonWithProgress(lesson, userId))
                .collectList()
                        .map(LessonListResponse::new));
    }

    public Mono<QuestionAnswerHistory> getQuestionAnswerHistory(String type, UUID questionId, UUID userId, Pageable pageable, Locale locale) {
        QuestionAnswerHistory history = new QuestionAnswerHistory();
        history.setQuestionId(questionId);
        history.setTextRu("Question details not available");
        history.setLessonId(null);
        history.setEntries(java.util.Collections.emptyList());
        history.setPage(pageable.getPageNumber());
        history.setSize(pageable.getPageSize());
        history.setTotal(0);
        return Mono.just(history);
    }

    private Mono<LessonItemDto> enrichLessonWithProgress(LessonItemResponse lesson, UUID userId) {
        LessonItemDto.LessonItemDtoBuilder builder = LessonItemDto.builder()
                .id(lesson.getId())
                .slug(lesson.getSlug())
                .titleRu(lesson.getTitleRu())
                .titleEn(lesson.getTitleEn())
                .descriptionRu(lesson.getDescriptionRu())
                .descriptionEn(lesson.getDescriptionEn())
                .lessonType(lesson.getLessonType())
                .difficulty(lesson.getDifficulty())
                .totalQuestions(lesson.getTotalQuestions())
                .totalWordsOwn(lesson.getWordCount());

                if (userId != null && LessonType.isVocabulary(lesson.getLessonType())) {
            return wordScoreRepository.countLearnedWords(userId, lesson.getId(), 80)
                    .map(learnedCount -> builder
                            .learnedWords(learnedCount.intValue())
                            .build());
        }

        // Для не-VOCABULARY квизов поля прогресса равны 0
        return Mono.just(builder
                .totalWordsOwn(0)
                .learnedWords(0)
                .build());
    }

    private Mono<VocabularyLessonDto> createVocabularyLesson(LessonItemResponse lessonItem,
                                                             List<VocabularyWordDto> vocabularyWords,
                                                             UUID userId) {
        VocabularyLessonDto lesson = new VocabularyLessonDto();
        lesson.setLessonId(lessonItem.getId());
        lesson.setSlug(lessonItem.getSlug());
        lesson.setTitleRu(lessonItem.getTitleRu());
        lesson.setTitleEn(lessonItem.getTitleEn());
        lesson.setDifficulty(lessonItem.getDifficulty().toString());
        lesson.setTotalWords(vocabularyWords.size());

        return Flux.fromIterable(vocabularyWords)
                .flatMap(word ->
                        userSessionService.getWordAnswers(userId, word.getId(), lessonItem.getId())
                                .map(answers -> {
                                    int nAll = answers.size();
                                    int nSuccess = (int) answers.stream()
                                            .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                                            .count();
                                    float successRate = nAll > 0 ? (float) nSuccess / nAll * 100f : 0f;

                                    VocabularyWordProgress progressItem = new VocabularyWordProgress();
                                    progressItem.setWordId(word.getId());
                                    progressItem.setWord(word.getWordIast());
                                    progressItem.setWordDevanagari(word.getWordDevanagari());
                                    progressItem.setTranslationRu(word.getTranslationRu());
                                    progressItem.setTranslationEn(word.getTranslationEn());
                                    progressItem.setNAll(nAll);
                                    progressItem.setNSuccess(nSuccess);
                                    progressItem.setSuccessRate(successRate);
                                    progressItem.setStatus(resolveStatus(successRate, nAll));
                                    return progressItem;
                                })
                                .onErrorReturn(emptyWordProgress(word))
                )
                .collectList()
                .map(wordProgressList -> {
                    lesson.setWords(wordProgressList);

                    int learnedWords = (int) wordProgressList.stream()
                            .filter(w -> WordStatus.LEARNING.equals(w.getStatus()))
                            .count();
                    lesson.setLearnedWords(learnedWords);
                    lesson.setProgressPercent(lesson.getTotalWords() > 0
                            ? (float) learnedWords / lesson.getTotalWords() * 100f
                            : 0f);
                    return lesson;
                });
    }

    private WordStatus resolveStatus(float successRate, int nAll) {
        if (nAll == 0) return WordStatus.NEW;
        if (successRate >= 80f) return WordStatus.MASTERED;
        return WordStatus.LEARNING;
    }

    private VocabularyWordProgress emptyWordProgress(VocabularyWordDto word) {
        VocabularyWordProgress p = new VocabularyWordProgress();
        p.setWordId(word.getId());
        p.setWord(word.getWordIast());
        p.setWordDevanagari(word.getWordDevanagari());
        p.setTranslationRu(word.getTranslationRu());
        p.setTranslationEn(word.getTranslationEn());
        p.setNSuccess(0);
        p.setNAll(0);
        p.setSuccessRate(0);
        p.setStatus(WordStatus.NEW);
        return p;
    }

    private Mono<WordAnswerHistory> createWordAnswerHistory(
            UUID wordId, UUID lessonId,
            String wordIast, UUID userId, Pageable pageable, Locale locale) {

        Mono<List<QuizAnswer>> answersMono =
                userSessionService.getWordAnswers(userId, wordId, lessonId);
        Mono<Long> totalMono =
                userSessionService.countWordAnswers(userId, wordId, lessonId);

        return Mono.zip(answersMono, totalMono)
                .map(tuple -> {
                    List<QuizAnswer> answers = tuple.getT1();
                    long total = tuple.getT2();

                    List<AnswerHistoryEntry> entries = answers.stream()
                            .map(qa -> {
                                AnswerHistoryEntry entry = new AnswerHistoryEntry();
                                entry.setAnsweredAt(qa.getAnsweredAt() != null
                                        ? java.time.LocalDateTime.ofInstant(qa.getAnsweredAt(), java.time.ZoneOffset.UTC)
                                        : null);
                                entry.setCorrectAnswer(qa.getCorrectFormIast());
                                entry.setUserAnswer(qa.getSelectedFormIast());
                                entry.setCorrect(Boolean.TRUE.equals(qa.getIsCorrect()));
                                return entry;
                            })
                            .collect(Collectors.toList());

                    return WordAnswerHistory.builder()
                            .wordId(wordId)
                            .lessonId(lessonId)
                            .word(wordIast)
                            .entries(entries)
                            .page(pageable.getPageNumber())
                            .size(pageable.getPageSize())
                            .total((int) total)
                            .build();
                });
    }
}
