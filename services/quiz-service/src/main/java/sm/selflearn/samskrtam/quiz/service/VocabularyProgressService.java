package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.mapper.QuizAnswerMapper;
import sm.selflearn.samskrtam.quiz.repository.WordScoreRepository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для работы с прогрессом по словарным урокам (vocabulary).
 * Выделен из LessonService для соблюдения Single Responsibility Principle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyProgressService {

    private final WordScoreRepository wordScoreRepository;
    private final UserSessionService userSessionService;
    private final QuizAnswerMapper quizAnswerMapper;

    /**
     * Обогащает список уроков прогрессом для vocabulary-уроков.
     */
    public Mono<LessonItemDto> enrichWithProgress(LessonItemResponse lesson, UUID userId) {
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
            return wordScoreRepository.countLearnedWords(
                            userId, lesson.getId(), (int) ProgressConstants.MASTERY_THRESHOLD)
                    .map(learnedCount -> builder
                            .learnedWords(learnedCount.intValue())
                            .build());
        }

        return Mono.just(builder
                .totalWordsOwn(0)
                .learnedWords(0)
                .build());
    }

    /**
     * Создаёт VocabularyLessonDto с прогрессом по каждому слову.
     */
    public Mono<VocabularyLessonDto> createVocabularyLesson(
            LessonItemResponse lessonItem,
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
                                    progressItem.setStatus(resolveWordStatus(successRate, nAll));
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

    /**
     * Создаёт WordAnswerHistory для конкретного слова.
     */
    public Mono<WordAnswerHistory> createWordAnswerHistory(
            UUID wordId, UUID lessonId, String wordIast,
            UUID userId, org.springframework.data.domain.Pageable pageable) {

        Mono<List<sm.selflearn.samskrtam.quiz.model.QuizAnswer>> answersMono =
                userSessionService.getWordAnswers(userId, wordId, lessonId);
        Mono<Long> totalMono =
                userSessionService.countWordAnswers(userId, wordId, lessonId);

        return Mono.zip(answersMono, totalMono)
                .map(tuple -> {
                    List<sm.selflearn.samskrtam.quiz.model.QuizAnswer> answers = tuple.getT1();
                    long total = tuple.getT2();

                    List<AnswerHistoryEntry> entries = answers.stream()
                            .map(quizAnswerMapper::toAnswerHistoryEntry)
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

    private WordStatus resolveWordStatus(float successRate, int nAll) {
        if (nAll == 0) return WordStatus.NEW;
        if (successRate >= ProgressConstants.MASTERY_THRESHOLD) return WordStatus.MASTERED;
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
}