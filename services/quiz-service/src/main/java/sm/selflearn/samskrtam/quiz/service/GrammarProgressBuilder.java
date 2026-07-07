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
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Строит полную GrammarLesson на основе основ (DeclensionStemDto)
 * и эталонных окончаний (CaseEndingDto) из content-service.
 *
 * Группирует вопросы по (gender, caseType, numberType),
 * агрегирует successRate через БД и заполняет локализованные поля.
 *
 * Делегирует создание {@link GrammarQuestionProgress} в {@link GrammarQuestionProgressFactory}
 * и поиск окончания в {@link CaseEndingMatcher}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GrammarProgressBuilder {

    private final QuizItemScoreRepository quizItemScoreRepository;
    private final ContentClient contentClient;
    private final GrammarQuestionProgressFactory progressFactory;

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

        // Найти case_ending.id для данной (gender, caseType, numberType)
        String genderStr = gender != null ? gender.name() : "UNSPECIFIED";
        UUID externalRefId = caseEndings.stream()
                .filter(ce -> matchingCaseEnding(ce, genderStr, form))
                .findFirst()
                .map(CaseEndingDto::getId)
                .orElse(null);

        if (externalRefId == null || userId == null) {
            return Mono.just(progressFactory.create(lessonItem, form, gender, caseEndings, 0f));
        }

        return quizItemScoreRepository
                .findByUserIdAndItemTypeAndExternalRefId(userId, ItemType.DECLENSION_FORM, externalRefId)
                .map(itemScore -> progressFactory.create(lessonItem, form, gender, caseEndings, itemScore.getScore()))
                .defaultIfEmpty(progressFactory.create(lessonItem, form, gender, caseEndings, 0f));
    }

    private boolean matchingCaseEnding(CaseEndingDto ce, String genderStr, DeclensionFormDto form) {
        String ceGender = ce.getGender() != null ? ce.getGender().name() : "UNSPECIFIED";
        return ceGender.equals(genderStr)
                && ce.getCaseType().name().equals(form.getCaseType().name())
                && ce.getNumberType().name().equals(form.getNumberType().name());
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