package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.repository.GrammarFormScoreRepository;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Строит полную GrammarLesson на основе основ (DeclensionStemDto)
 * и эталонных окончаний (CaseEndingDto) из content-service.
 *
 * Группирует вопросы по (gender, caseType, numberType),
 * агрегирует successRate через БД и заполняет локализованные поля.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GrammarProgressBuilder {

    private final GrammarFormScoreRepository grammarFormScoreRepository;
    private final ContentClient contentClient;

    /**
     * Основной метод: строит GrammarLesson для заданного slug.
     */
    public Mono<GrammarLesson> build(String slug, UUID userId) {
        return contentClient.getLessonItemBySlug(slug)
                .flatMap(lessonItem ->
                        contentClient.getDeclensionStemsForLesson(slug)
                                .flatMap(stems -> processStems(lessonItem, stems, userId))
                );
    }

    private Mono<GrammarLesson> processStems(LessonItemResponse lessonItem,
                                              List<DeclensionStemDto> stems,
                                              UUID userId) {
        if (stems.isEmpty()) {
            return Mono.just(emptyLesson(lessonItem));
        }
        VowelType vowelType = stems.get(0).getVowelType();
        return contentClient.getCaseEndingsByVowelType(String.valueOf(vowelType))
                .flatMap(caseEndings -> processGroups(lessonItem, stems, vowelType, caseEndings, userId));
    }

    private Mono<GrammarLesson> processGroups(LessonItemResponse lessonItem,
                                               List<DeclensionStemDto> stems,
                                               VowelType vowelType,
                                               List<CaseEndingDto> caseEndings,
                                               UUID userId) {
        Set<String> groupGenders = new LinkedHashSet<>();
        for (DeclensionStemDto stem : stems) {
            String g = stem.getGender() != null ? stem.getGender().name() : "UNSPECIFIED";
            groupGenders.add(g);
        }

        return contentClient.getDeclensionForms(stems.get(0).getId())
                .flatMapMany(allForms -> {
                    List<Mono<GrammarQuestionProgress>> groupMonos = new ArrayList<>();
                    for (String g : groupGenders) {
                        Gender genderEnum = parseGender(g);
                        for (DeclensionFormDto form : allForms) {
                            groupMonos.add(buildGroupProgress(
                                    lessonItem, vowelType, caseEndings,
                                    genderEnum, form, userId));
                        }
                    }
                    return Flux.merge(groupMonos);
                })
                .collectList()
                .map(allGroups -> assembleGrammarLesson(lessonItem, allGroups));
    }

    private Gender parseGender(String genderStr) {
        if (genderStr == null) return null;
        try {
            return Gender.valueOf(genderStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Mono<GrammarQuestionProgress> buildGroupProgress(
            LessonItemResponse lessonItem, VowelType vowelType,
            List<CaseEndingDto> caseEndings,
            Gender gender, DeclensionFormDto form,
            UUID userId) {

        String genderStr = gender != null ? gender.name() : "UNSPECIFIED";
        String caseStr = form.getCaseType().name();
        String numberStr = form.getNumberType().name();
        UUID questionId = deterministicId(genderStr, caseStr, numberStr);

        return grammarFormScoreRepository.aggregateSuccessRate(
                        userId, lessonItem.getId(), genderStr, caseStr, numberStr)
                .map(avgScore -> {
                    float successRate = avgScore.floatValue();
                    GrammarQuestionProgress p = new GrammarQuestionProgress();
                    p.setQuestionId(questionId);
                    p.setTextRu(form.getCaseType().getRuName() + ", " + form.getNumberType().getRuName());
                    p.setTextEn(form.getCaseType().getEnName() + ", " + form.getNumberType().getEnName());
                    p.setSuccessRate(successRate);
                    p.setStatus(resolveGrammarStatus(successRate));

                    p.setCaseType(caseStr);
                    p.setCaseRu(form.getCaseType().getRuName());
                    p.setCaseEn(form.getCaseType().getEnName());
                    p.setNumberType(numberStr);
                    p.setNumberRu(form.getNumberType().getRuName());
                    p.setNumberEn(form.getNumberType().getEnName());
                    p.setGender(genderStr);
                    if (gender != null) {
                        p.setGenderRu(gender.getRuName());
                        p.setGenderEn(gender.getEnName());
                        } else {
                            p.setGenderRu(null);
                            p.setGenderEn(null);
                        }

                        // Match case ending
                        p.setCaseEnding(findCaseEnding(gender, form, caseEndings));
                        return p;
                })
                .defaultIfEmpty(emptyGroupProgress(lessonItem, form, gender, questionId, caseEndings));
    }

    private UUID deterministicId(String gender, String caseType, String numberType) {
        return UUID.nameUUIDFromBytes(
                (gender + ":" + caseType + ":" + numberType).getBytes(StandardCharsets.UTF_8));
    }

    private WordStatus resolveGrammarStatus(float score) {
        if (score == 0) return WordStatus.NEW;
        if (score < ProgressConstants.GRAMMAR_LEARNING_THRESHOLD) return WordStatus.REVIEW;
        if (score < ProgressConstants.MASTERY_THRESHOLD) return WordStatus.LEARNING;
        return WordStatus.MASTERED;
    }

    private GrammarQuestionProgress emptyGroupProgress(
            LessonItemResponse lessonItem, DeclensionFormDto form,
            Gender gender, UUID questionId, List<CaseEndingDto> caseEndings) {

        GrammarQuestionProgress p = new GrammarQuestionProgress();
        p.setQuestionId(questionId);
        p.setTextRu(form.getCaseType().getRuName() + ", " + form.getNumberType().getRuName());
        p.setTextEn(form.getCaseType().getEnName() + ", " + form.getNumberType().getEnName());
        p.setSuccessRate(0f);
        p.setStatus(WordStatus.NEW);

        p.setCaseType(form.getCaseType().name());
        p.setCaseRu(form.getCaseType().getRuName());
        p.setCaseEn(form.getCaseType().getEnName());
        p.setNumberType(form.getNumberType().name());
        p.setNumberRu(form.getNumberType().getRuName());
        p.setNumberEn(form.getNumberType().getEnName());
        String genderStr = gender != null ? gender.name() : "UNSPECIFIED";
        p.setGender(genderStr);
        if (gender != null) {
            p.setGenderRu(gender.getRuName());
            p.setGenderEn(gender.getEnName());
        } else {
            p.setGenderRu(null);
            p.setGenderEn(null);
        }
        p.setCaseEnding(findCaseEnding(gender, form, caseEndings));
        return p;
    }

    private String findCaseEnding(Gender gender, DeclensionFormDto form, List<CaseEndingDto> caseEndings) {
        // Convert shared enums to content-service enums (same constant names)
        String targetCaseType = form.getCaseType().name();
                String targetNumberType = form.getNumberType().name();
                String targetGender = gender != null ? gender.name() : null;

                // 1. Точный матч по gender + caseType + numberType
                for (CaseEndingDto ce : caseEndings) {
                    boolean genderMatch = targetGender == null ||
                            ce.getGender() == null ||
                            targetGender.equals(ce.getGender().name());
                    if (genderMatch &&
                            targetCaseType.equals(ce.getCaseType().name()) &&
                            targetNumberType.equals(ce.getNumberType().name())) {
                        return ce.getEndingIast();
                    }
                }

                // 2. Fallback: матч без учёта gender для уроков -i, -u, -r
                for (CaseEndingDto ce : caseEndings) {
                    if (targetCaseType.equals(ce.getCaseType().name()) &&
                            targetNumberType.equals(ce.getNumberType().name())) {
                        return ce.getEndingIast();
                    }
                }

        log.warn("No matching case ending found for gender={}, caseType={}, numberType={}",
                targetGender, targetCaseType, targetNumberType);
        return null;
    }

    private GrammarLesson emptyLesson(LessonItemResponse lessonItem) {
        GrammarLesson lesson = new GrammarLesson();
        lesson.setLessonId(lessonItem.getId());
        lesson.setType(lessonItem.getLessonType() != null ? lessonItem.getLessonType().name() : null);
        lesson.setTitleRu(lessonItem.getTitleRu());
        lesson.setTitleEn(lessonItem.getTitleEn());
        lesson.setDifficulty(lessonItem.getDifficulty() != null ? lessonItem.getDifficulty().toString() : null);
        lesson.setTotalQuestions(0);
        lesson.setLearnedQuestions(0);
        lesson.setProgressPercent(0f);
        lesson.setQuestions(Collections.emptyList());
        return lesson;
    }

    private GrammarLesson assembleGrammarLesson(
            LessonItemResponse lessonItem, List<GrammarQuestionProgress> allGroups) {
        Map<UUID, GrammarQuestionProgress> byGroup = new LinkedHashMap<>();
        for (GrammarQuestionProgress p : allGroups) {
            byGroup.merge(p.getQuestionId(), p,
                    (existing, newVal) -> existing.getSuccessRate() >= newVal.getSuccessRate()
                            ? existing : newVal);
        }
        List<GrammarQuestionProgress> deduplicated = new ArrayList<>(byGroup.values());
        int total = deduplicated.size();
        int learned = (int) deduplicated.stream()
                .filter(p -> WordStatus.MASTERED.equals(p.getStatus())).count();

        GrammarLesson lesson = new GrammarLesson();
        lesson.setLessonId(lessonItem.getId());
        lesson.setType(lessonItem.getLessonType() != null ? lessonItem.getLessonType().name() : null);
        lesson.setTitleRu(lessonItem.getTitleRu());
        lesson.setTitleEn(lessonItem.getTitleEn());
        lesson.setDifficulty(lessonItem.getDifficulty() != null ? lessonItem.getDifficulty().toString() : null);
        lesson.setTotalQuestions(total);
        lesson.setLearnedQuestions(learned);
        lesson.setProgressPercent(total > 0 ? (float) learned / total * 100f : 0f);
        lesson.setQuestions(deduplicated);
        return lesson;
    }
}