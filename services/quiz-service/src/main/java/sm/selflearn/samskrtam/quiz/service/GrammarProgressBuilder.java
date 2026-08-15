package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import sm.selflearn.samskrtam.content.dto.*;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.quiz.dto.*;
import sm.selflearn.samskrtam.quiz.model.ItemType;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;
import sm.selflearn.samskrtam.quiz.repository.QuizItemScoreRepository;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Строит полную GrammarLesson на основе основ (DeclensionStemDto)
 * и эталонных окончаний (CaseEndingDto) из content-service.
 *
 * Группирует вопросы по (gender, caseType, numberType),
 * агрегирует score через БД и заполняет локализованные поля.
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
    private final GrammarProgressAggregationService aggregationService;

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
        // Use slug-based endpoint to fetch case endings for ALL vowel types
        // (supports compound lessons like declensions-i-u, declensions-ii-uu)
        String slug = lessonItem.getSlug();
        return contentClient.getCaseEndingsForLesson(slug, null, null, null)
                .flatMap(caseEndings -> processGroups(lessonItem, stems, caseEndings, userId));
    }

        private Mono<GrammarLesson> processGroups(LessonItemResponse lessonItem,
                                               List<DeclensionStemDto> stems,
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
                                    lessonItem, caseEndings,
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
            LessonItemResponse lessonItem,
            List<CaseEndingDto> caseEndings,
            Gender gender, DeclensionFormDto form,
            UUID userId) {

        // Найти case_ending.id для данной (gender, caseType, numberType)
        String genderStr = gender != null ? gender.name() : "UNSPECIFIED";
        String caseType = form.getCaseType().name();
        String numberType = form.getNumberType().name();
        String progressTag = caseType + "|" + numberType + "|" + genderStr;

        if (userId == null) {
            return Mono.just(progressFactory.create(lessonItem, form, gender, caseEndings, 0));
        }

        Instant now = Instant.now();
        return quizItemScoreRepository
                .findByUserIdAndItemTypeAndProgressTag(userId, ItemType.DECLENSION_FORM, progressTag)
                .map(itemScore -> progressFactory.create(lessonItem, form, gender, caseEndings, itemScore, now))
                .defaultIfEmpty(progressFactory.create(lessonItem, form, gender, caseEndings, 0));
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
        lesson.setStatusSummary(new LessonStatusSummary(0, 0, 0, 0, 0));
        return lesson;
    }

        private GrammarLesson assembleGrammarLesson(
            LessonItemResponse lessonItem, List<GrammarQuestionProgress> allGroups) {
        Map<UUID, GrammarQuestionProgress> byGroup = new LinkedHashMap<>();
        for (GrammarQuestionProgress p : allGroups) {
            byGroup.merge(p.getQuestionId(), p,
                    (existing, newVal) -> existing.getScore() >= newVal.getScore()
                            ? existing : newVal);
        }
        List<GrammarQuestionProgress> deduplicated = new ArrayList<>(byGroup.values());

        int distinctCells = (int) deduplicated.stream()
                .map(q -> q.getCaseType() + ":" + q.getNumberType())
                .distinct()
                .count();
        int newCount = 0;
        int learning = 0;
        int mastered = 0;
        int reviewDue = 0;

        for (GrammarQuestionProgress q : deduplicated) {
            switch (q.getStatus()) {
                case NEW -> newCount++;
                case LEARNING -> learning++;
                case MASTERED -> mastered++;
                case REVIEW -> reviewDue++;
            }
        }

        int learned = mastered + reviewDue;

        GrammarLesson lesson = new GrammarLesson();
        lesson.setLessonId(lessonItem.getId());
        lesson.setType(lessonItem.getLessonType() != null ? lessonItem.getLessonType().name() : null);
        lesson.setTitleRu(lessonItem.getTitleRu());
        lesson.setTitleEn(lessonItem.getTitleEn());
        lesson.setDifficulty(lessonItem.getDifficulty() != null ? lessonItem.getDifficulty().toString() : null);
        lesson.setTotalQuestions(distinctCells);
        lesson.setLearnedQuestions(learned);
        lesson.setProgressPercent(distinctCells > 0 ? (float) learned / distinctCells * 100f : 0f);
        lesson.setStatusSummary(new LessonStatusSummary(distinctCells, newCount, learning, mastered, reviewDue));

        List<GrammarProgressAggregationService.ItemAgg> aggInput = deduplicated.stream()
                .map(GrammarProgressAggregationService::from)
                .collect(Collectors.toList());
        GrammarProgressAggregationService.GrammarProgressAggregations aggregations =
                aggregationService.aggregate(aggInput);
        lesson.setCaseAggregations(aggregations.caseAggregations());
        lesson.setNumberAggregations(aggregations.numberAggregations());
        lesson.setGrid(aggregations.grid());
        lesson.setPairAggregations(aggregations.pairAggregations());
        return lesson;
    }
}