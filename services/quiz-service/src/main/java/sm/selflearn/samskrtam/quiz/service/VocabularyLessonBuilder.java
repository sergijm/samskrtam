package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.dto.VocabularyWordDto;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Строит {@link VocabularyLessonDto} с прогрессом по каждому слову.
 * Статус и {@link LessonStatusSummary} вычисляются за один проход через
 * {@link QuizItemScoreRepository}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VocabularyLessonBuilder {

    private final QuizItemScoreRepository quizItemScoreRepository;
    private final WordStatusResolver wordStatusResolver;

    /**
     * Создаёт {@link VocabularyLessonDto} с прогрессом по каждому слову.
     *
     * @param lessonItem      метаданные урока из content-service
     * @param vocabularyWords список слов урока
     * @param userId          id пользователя (null для анонима)
     * @return Mono с заполненным VocabularyLessonDto
     */
    public Mono<VocabularyLessonDto> build(
            LessonItemResponse lessonItem,
            List<VocabularyWordDto> vocabularyWords,
            UUID userId) {

        VocabularyLessonDto lesson = initLesson(lessonItem, vocabularyWords.size());

        log.info("VocabularyLessonBuilder: slug={}, userId={}, wordCount={}",
                lessonItem.getSlug(), userId, vocabularyWords.size());

        if (userId == null) {
            lesson.setLearnedWords(0);
            lesson.setProgressPercent(0f);
            lesson.setWords(vocabularyWords.stream()
                    .map(this::emptyWordProgress)
                    .collect(Collectors.toList()));
            return Mono.just(lesson);
        }

        List<String> progressTags = vocabularyWords.stream()
                .map(VocabularyWordDto::getWordIast)
                .collect(Collectors.toList());

        Instant now = Instant.now();

        return quizItemScoreRepository
                .findByUserIdAndItemTypeAndProgressTagIn(userId, ItemType.VOCABULARY_WORD, progressTags)
                .collectMap(QuizItemScore::getProgressTag, score -> score)
                .map(scoresMap -> populateLesson(lesson, vocabularyWords, scoresMap, now));
    }

    private VocabularyLessonDto initLesson(LessonItemResponse lessonItem, int totalWords) {
        VocabularyLessonDto lesson = new VocabularyLessonDto();
        lesson.setLessonId(lessonItem.getId());
        lesson.setSlug(lessonItem.getSlug());
        lesson.setTitleRu(lessonItem.getTitleRu());
        lesson.setTitleEn(lessonItem.getTitleEn());
        lesson.setDifficulty(lessonItem.getDifficulty().toString());
        lesson.setTotalWords(totalWords);
        return lesson;
    }

    private VocabularyLessonDto populateLesson(
            VocabularyLessonDto lesson,
            List<VocabularyWordDto> vocabularyWords,
            java.util.Map<String, QuizItemScore> scoresMap,
            Instant now) {

        int newCount = 0;
        int learning = 0;
        int mastered = 0;
        int reviewDue = 0;

        List<VocabularyWordProgress> wordProgressList = new ArrayList<>();

        for (VocabularyWordDto word : vocabularyWords) {
            QuizItemScore itemScore = scoresMap.get(word.getWordIast());
            WordStatus status = wordStatusResolver.resolve(itemScore, now);

            switch (status) {
                case NEW -> newCount++;
                case LEARNING -> learning++;
                case MASTERED -> mastered++;
                case REVIEW -> reviewDue++;
            }

            VocabularyWordProgress progressItem = toWordProgress(word, status, itemScore);
            wordProgressList.add(progressItem);
        }

        log.info("VocabularyLessonBuilder: found scores in DB={}, total words={}",
                scoresMap.size(), vocabularyWords.size());

        lesson.setWords(wordProgressList);

        int masteredTotal = mastered + reviewDue;
        lesson.setLearnedWords(masteredTotal);
        lesson.setProgressPercent(vocabularyWords.size() > 0
                ? (float) masteredTotal / vocabularyWords.size() * 100f
                : 0f);
        lesson.setStatusSummary(new LessonStatusSummary(
                vocabularyWords.size(), newCount, learning, mastered, reviewDue));

        return lesson;
    }

    private VocabularyWordProgress toWordProgress(
            VocabularyWordDto word, WordStatus status, QuizItemScore itemScore) {
        VocabularyWordProgress p = new VocabularyWordProgress();
        p.setWordId(word.getId());
        p.setWord(word.getWordIast());
        p.setWordDevanagari(word.getWordDevanagari());
        p.setTranslationRu(word.getTranslationRu());
        p.setTranslationEn(word.getTranslationEn());
        p.setStatus(status);
        if (itemScore != null) {
            p.setScore(itemScore.getScore());
        }
        return p;
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
