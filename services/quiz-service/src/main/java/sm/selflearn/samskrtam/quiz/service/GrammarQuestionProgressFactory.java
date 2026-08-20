package sm.selflearn.samskrtam.quiz.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.content.dto.LessonItemResponse;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.quiz.constants.ProgressConstants;
import sm.selflearn.samskrtam.quiz.dto.GrammarQuestionProgress;
import sm.selflearn.samskrtam.quiz.dto.WordStatus;
import sm.selflearn.samskrtam.quiz.localization.CaseNumberGenderLocalizer;
import sm.selflearn.samskrtam.quiz.model.QuizItemScore;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Фабрика для создания {@link GrammarQuestionProgress} — устраняет дублирование
 * между buildGroupProgress и emptyGroupProgress.
 * Статус вычисляется через общий {@link WordStatusResolver}.
 */
@Component
@RequiredArgsConstructor
public class GrammarQuestionProgressFactory {

    private final CaseEndingMatcher caseEndingMatcher;
    private final WordStatusResolver wordStatusResolver;

    /**
     * Создаёт {@link GrammarQuestionProgress} с заданным score (без REVIEW).
     */
    public GrammarQuestionProgress create(
            LessonItemResponse lessonItem,
            DeclensionFormDto form,
            Gender gender,
            List<CaseEndingDto> caseEndings,
            int score) {
        return build(lessonItem, form, gender, caseEndings, score, null);
    }

    /**
     * Создаёт {@link GrammarQuestionProgress} из {@link QuizItemScore} (с учётом REVIEW).
     */
    public GrammarQuestionProgress create(
            LessonItemResponse lessonItem,
            DeclensionFormDto form,
            Gender gender,
            List<CaseEndingDto> caseEndings,
            QuizItemScore scoreEntity,
            Instant now) {
        WordStatus status = wordStatusResolver.resolve(scoreEntity, now);
        int score = scoreEntity != null ? scoreEntity.getScore() : 0;
        return build(lessonItem, form, gender, caseEndings, score, status);
    }

    private GrammarQuestionProgress build(
            LessonItemResponse lessonItem,
            DeclensionFormDto form,
            Gender gender,
            List<CaseEndingDto> caseEndings,
            int score,
            WordStatus forcedStatus) {

        String genderStr = gender != null ? gender.name() : "UNSPECIFIED";
        UUID questionId = deterministicId(genderStr, form.getCaseType().name(), form.getNumberType().name());

        GrammarQuestionProgress p = new GrammarQuestionProgress();
        p.setQuestionId(questionId);
        p.setTextRu(CaseNumberGenderLocalizer.caseTypeRu(form.getCaseType()) + ", " +
                     CaseNumberGenderLocalizer.numberTypeRu(form.getNumberType()));
        p.setTextEn(CaseNumberGenderLocalizer.caseTypeEn(form.getCaseType()) + ", " +
                     CaseNumberGenderLocalizer.numberTypeEn(form.getNumberType()));
        p.setScore(score);
        p.setStatus(forcedStatus != null ? forcedStatus : resolveGrammarStatus(score));

        p.setCaseType(form.getCaseType().name());
        p.setCaseRu(CaseNumberGenderLocalizer.caseTypeRu(form.getCaseType()));
        p.setCaseEn(CaseNumberGenderLocalizer.caseTypeEn(form.getCaseType()));
        p.setNumberType(form.getNumberType().name());
        p.setNumberRu(CaseNumberGenderLocalizer.numberTypeRu(form.getNumberType()));
        p.setNumberEn(CaseNumberGenderLocalizer.numberTypeEn(form.getNumberType()));
        p.setGender(genderStr);
        p.setGenderRu(CaseNumberGenderLocalizer.genderRu(gender));
        p.setGenderEn(CaseNumberGenderLocalizer.genderEn(gender));
        p.setCaseEnding(caseEndingMatcher.find(gender, form, caseEndings));
        return p;
    }

    private UUID deterministicId(String gender, String caseType, String numberType) {
        return UUID.nameUUIDFromBytes(
                (gender + ":" + caseType + ":" + numberType).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Вычисляет статус по хранимому score.
     * Используется для GrammarQuestionProgress, где статус определяется по среднему score.
     *
     * @param score значение 0-100, где 0 = нет попыток
     * @return статус по единому порогу
     */
    private WordStatus resolveGrammarStatus(int score) {
        if (score == 0) return WordStatus.NEW;
        if (score < ProgressConstants.MASTERED_LOWER_THRESHOLD) return WordStatus.LEARNING;
        return WordStatus.MASTERED;
    }
}

