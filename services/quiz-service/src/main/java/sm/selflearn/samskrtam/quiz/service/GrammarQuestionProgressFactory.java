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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Фабрика для создания {@link GrammarQuestionProgress} — устраняет дублирование
 * между buildGroupProgress и emptyGroupProgress.
 */
@Component
@RequiredArgsConstructor
public class GrammarQuestionProgressFactory {

    private final CaseEndingMatcher caseEndingMatcher;

    /**
     * Создаёт {@link GrammarQuestionProgress} с заданными параметрами.
     */
    public GrammarQuestionProgress create(
            LessonItemResponse lessonItem,
            DeclensionFormDto form,
            Gender gender,
            List<CaseEndingDto> caseEndings,
            float successRate) {

        String genderStr = gender != null ? gender.name() : "UNSPECIFIED";
        UUID questionId = deterministicId(genderStr, form.getCaseType().name(), form.getNumberType().name());

        GrammarQuestionProgress p = new GrammarQuestionProgress();
        p.setQuestionId(questionId);
        p.setTextRu(form.getCaseType().getRuName() + ", " + form.getNumberType().getRuName());
        p.setTextEn(form.getCaseType().getEnName() + ", " + form.getNumberType().getEnName());
        p.setSuccessRate(successRate);
        p.setStatus(resolveGrammarStatus(successRate));

        p.setCaseType(form.getCaseType().name());
        p.setCaseRu(form.getCaseType().getRuName());
        p.setCaseEn(form.getCaseType().getEnName());
        p.setNumberType(form.getNumberType().name());
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

        p.setCaseEnding(caseEndingMatcher.find(gender, form, caseEndings));
        return p;
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
}