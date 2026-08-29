package sm.selflearn.samskrtam.quiz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.dto.CaseEndingDto;
import sm.selflearn.samskrtam.content.dto.DeclensionFormDto;
import sm.selflearn.samskrtam.morphology.Gender;

import java.util.List;

/**
 * Ищет подходящее окончание (endingIast) среди эталонных CaseEndingDto
 * по трём критериям: gender (опционально), caseType, numberType.
 */
@Component
@Slf4j
public class CaseEndingMatcher {

    /**
     * Пытается найти окончание для заданной комбинации gender/caseType/numberType.
     * <p>
     * 1. Точный матч по gender + caseType + numberType
     * 2. Fallback: матч без учёта gender (для уроков -i, -u, -r)
     */
    public String find(Gender gender, DeclensionFormDto form, List<CaseEndingDto> caseEndings) {
        String targetCaseType = form.getCaseType().name();
        String targetNumberType = form.getNumberType().name();
        String targetGender = gender != null ? gender.name() : null;

        // 1. Точный матч по gender + caseType + numberType
        for (CaseEndingDto ce : caseEndings) {
            boolean genderMatch = targetGender == null
                    || ce.getGender() == null
                    || targetGender.equals(ce.getGender().name());
            if (genderMatch
                    && targetCaseType.equals(ce.getCaseType().name())
                    && targetNumberType.equals(ce.getNumberType().name())) {
                return ce.getEndingIast();
            }
        }

        // 2. Fallback: матч без учёта gender
        for (CaseEndingDto ce : caseEndings) {
            if (targetCaseType.equals(ce.getCaseType().name())
                    && targetNumberType.equals(ce.getNumberType().name())) {
                return ce.getEndingIast();
            }
        }

        log.warn("No matching case ending found for gender={}, caseType={}, numberType={}",
                targetGender, targetCaseType, targetNumberType);
        return null;
    }
}