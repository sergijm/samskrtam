package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.repository.GrammarFormScoreRepository;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Сервис для работы с прогрессом по грамматическим урокам (declensions).
 * Выделен из LessonService для соблюдения Single Responsibility Principle.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GrammarProgressService {

    private final GrammarFormScoreRepository grammarFormScoreRepository;
    private final ContentClient contentClient;

    /**
     * Обогащает список уроков прогрессом для грамматических уроков.
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

        if (userId != null && !LessonType.isVocabulary(lesson.getLessonType())) {
            return grammarFormScoreRepository.countLearnedForms(
                            userId, lesson.getId(), (int) ProgressConstants.MASTERY_THRESHOLD)
                    .map(count -> builder
                            .learnedWords(count.intValue())
                            .totalWordsOwn(24)
                            .build());
        }

        return Mono.just(builder
                .totalWordsOwn(0)
                .learnedWords(0)
                .build());
    }

    /**
     * Строит GrammarLesson с прогрессом по каждой форме склонения.
     */
    public Mono<GrammarLesson> getGrammarLesson(String slug, UUID userId) {
        return contentClient.getLessonItemBySlug(slug)
                .flatMap(lessonItem ->
                        contentClient.getDeclensionStemsForLesson(slug)
                                .flatMap(stems ->
                                        Flux.fromIterable(stems)
                                                .flatMap(stem ->
                                                        contentClient.getDeclensionForms(stem.getId())
                                                                .flatMapMany(Flux::fromIterable)
                                                                .flatMap(form -> buildFormProgress(
                                                                        stem, form, userId, lessonItem.getId()))
                                                )
                                                .collectList()
                                                .map(allForms -> assembleGrammarLesson(lessonItem, allForms))
                                )
                );
    }

    private Mono<GrammarQuestionProgress> buildFormProgress(
            DeclensionStemDto stem, DeclensionFormDto form, UUID userId, UUID lessonId) {
        String caseStr = form.getCaseType().name();
        String numberStr = form.getNumberType().name();
        return grammarFormScoreRepository
                .findByUserIdAndLessonIdAndCaseTypeAndNumberType(userId, lessonId, caseStr, numberStr)
                .map(scoreEntry -> {
                    float successRate = scoreEntry.getScore();
                    GrammarQuestionProgress p = new GrammarQuestionProgress();
                    p.setQuestionId(deterministicId(stem.getId(), caseStr, numberStr));
                    p.setTextRu(form.getCaseType().getRuName() + ", " + form.getNumberType().getRuName());
                    p.setTextEn(form.getCaseType().getEnName() + ", " + form.getNumberType().getEnName());
                    p.setCorrectAnswerRu(form.getFormIast());
                    p.setCorrectAnswerEn(form.getFormIast());
                    p.setSuccessRate(successRate);
                    p.setStatus(resolveGrammarStatus(successRate));
                    return p;
                })
                .defaultIfEmpty(emptyProgress(stem, form));
    }

    private UUID deterministicId(UUID stemId, String caseType, String numberType) {
        return UUID.nameUUIDFromBytes(
                (stemId + ":" + caseType + ":" + numberType).getBytes(StandardCharsets.UTF_8));
    }

    private WordStatus resolveGrammarStatus(float score) {
        if (score == 0) return WordStatus.NEW;
        if (score < ProgressConstants.GRAMMAR_LEARNING_THRESHOLD) return WordStatus.REVIEW;
        if (score < ProgressConstants.MASTERY_THRESHOLD) return WordStatus.LEARNING;
        return WordStatus.MASTERED;
    }

    private GrammarQuestionProgress emptyProgress(DeclensionStemDto stem, DeclensionFormDto form) {
        String caseStr = form.getCaseType().name();
        String numberStr = form.getNumberType().name();
        GrammarQuestionProgress p = new GrammarQuestionProgress();
        p.setQuestionId(deterministicId(stem.getId(), caseStr, numberStr));
        p.setTextRu(form.getCaseType().getRuName() + ", " + form.getNumberType().getRuName());
        p.setTextEn(form.getCaseType().getEnName() + ", " + form.getNumberType().getEnName());
        p.setCorrectAnswerRu(form.getFormIast());
        p.setCorrectAnswerEn(form.getFormIast());
        p.setSuccessRate(0f);
        p.setStatus(WordStatus.NEW);
        return p;
    }

    private GrammarLesson assembleGrammarLesson(
            LessonItemResponse lessonItem, List<GrammarQuestionProgress> allForms) {
        // Де-дублируем по questionId (детерминирован по case+number)
        Map<UUID, GrammarQuestionProgress> byPair = new LinkedHashMap<>();
        for (GrammarQuestionProgress p : allForms) {
            byPair.merge(p.getQuestionId(), p,
                    (existing, newVal) -> existing.getSuccessRate() >= newVal.getSuccessRate()
                            ? existing : newVal);
        }
        List<GrammarQuestionProgress> deduplicated = new ArrayList<>(byPair.values());
        int total = deduplicated.size();
        int learned = (int) deduplicated.stream()
                .filter(p -> WordStatus.MASTERED.equals(p.getStatus())).count();

        GrammarLesson lesson = new GrammarLesson();
        lesson.setLessonId(lessonItem.getId());
        lesson.setType(lessonItem.getLessonType().name());
        lesson.setTitleRu(lessonItem.getTitleRu());
        lesson.setTitleEn(lessonItem.getTitleEn());
        lesson.setDifficulty(lessonItem.getDifficulty().toString());
        lesson.setTotalQuestions(total);
        lesson.setLearnedQuestions(learned);
        lesson.setProgressPercent(total > 0 ? (float) learned / total * 100f : 0f);
        lesson.setQuestions(deduplicated);
        return lesson;
    }
}