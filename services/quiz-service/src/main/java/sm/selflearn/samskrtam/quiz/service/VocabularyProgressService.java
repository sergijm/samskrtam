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
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для работы с прогрессом по словарным урокам (vocabulary).
 * Выделен из LessonService для соблюдения Single Responsibility Principle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VocabularyProgressService {

    private final QuizItemScoreRepository quizItemScoreRepository;
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
            return quizItemScoreRepository.countLearnedItems(
                            userId, ItemType.VOCABULARY_WORD, ProgressConstants.MASTERED_LOWER_THRESHOLD)
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
     * Статус и LessonStatusSummary вычисляются за один проход через QuizItemScoreRepository
     * без отдельного запроса к word_score.
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

        if (userId == null) {
            lesson.setLearnedWords(0);
            lesson.setProgressPercent(0f);
            lesson.setWords(vocabularyWords.stream()
                    .map(this::emptyWordProgress)
                    .collect(Collectors.toList()));
            return Mono.just(lesson);
        }

        List<UUID> wordIds = vocabularyWords.stream()
                .map(VocabularyWordDto::getId)
                .collect(Collectors.toList());

        Instant now = Instant.now();

        return quizItemScoreRepository
                .findByUserIdAndItemTypeAndExternalRefIdIn(userId, ItemType.VOCABULARY_WORD, wordIds)
                .collectMap(QuizItemScore::getExternalRefId, score -> score)
                .map(scoresMap -> {
                    int newCount = 0;
                    int learning = 0;
                    int mastered = 0;
                    int reviewDue = 0;

                    List<VocabularyWordProgress> wordProgressList = new ArrayList<>();

                    for (VocabularyWordDto word : vocabularyWords) {
                        QuizItemScore itemScore = scoresMap.get(word.getId());
                        WordStatus status = resolveStatus(itemScore, now);

                        switch (status) {
                            case NEW -> newCount++;
                            case LEARNING -> learning++;
                            case MASTERED -> mastered++;
                            case REVIEW -> reviewDue++;
                        }

                        VocabularyWordProgress progressItem = new VocabularyWordProgress();
                        progressItem.setWordId(word.getId());
                        progressItem.setWord(word.getWordIast());
                        progressItem.setWordDevanagari(word.getWordDevanagari());
                        progressItem.setTranslationRu(word.getTranslationRu());
                        progressItem.setTranslationEn(word.getTranslationEn());
                        progressItem.setStatus(status);
                        if (itemScore != null) {
                            progressItem.setScore(itemScore.getScore());
                        }
                        wordProgressList.add(progressItem);
                    }

                    lesson.setWords(wordProgressList);
                    int masteredTotal = mastered + reviewDue;
                    lesson.setLearnedWords(masteredTotal);
                    lesson.setProgressPercent(vocabularyWords.size() > 0
                            ? (float) masteredTotal / vocabularyWords.size() * 100f
                            : 0f);
                    lesson.setStatusSummary(new LessonStatusSummary(
                            vocabularyWords.size(), newCount, learning, mastered, reviewDue));

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

        /**
     * Вычисляет статус QuizItem по хранимому score и nextReviewAt.
     *
     * @param itemScore строка из quiz_item_score или null
     * @param now текущее время
     * @return статус по правилам ADR-007
     */
    private WordStatus resolveStatus(QuizItemScore itemScore, Instant now) {
        if (itemScore == null) {
            return WordStatus.NEW;
        }
        if (itemScore.getScore() < ProgressConstants.MASTERED_LOWER_THRESHOLD) {
            return WordStatus.LEARNING;
        }
        // score >= MASTERED_LOWER_THRESHOLD
        if (itemScore.getNextReviewAt() != null && !itemScore.getNextReviewAt().isAfter(now)) {
            return WordStatus.REVIEW;
        }
        return WordStatus.MASTERED;
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
        p.setScore(0);
        p.setStatus(WordStatus.NEW);
        return p;
    }
}