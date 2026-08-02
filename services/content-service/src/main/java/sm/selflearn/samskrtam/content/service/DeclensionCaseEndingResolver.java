package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.model.CaseEnding;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.content.repository.CaseEndingRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeclensionCaseEndingResolver {

    private final CaseEndingRepository caseEndingRepository;

    /**
     * Определяет CaseEnding для (vowelType, gender, caseType, numberType).
     * Для основ -i, -u, -ṛ ищет UNSPECIFIED gender.
     *
     * @return найденный CaseEnding или null
     */
    public CaseEnding resolveCaseEnding(VowelType vowelType, Gender gender, CaseType caseType, NumberType numberType) {
        if (gender == null) {
            gender = Gender.UNSPECIFIED;
        }
        boolean isUnspecifiedGenderType = (vowelType == VowelType.I_STEM
                || vowelType == VowelType.II_STEM
                || vowelType == VowelType.U_STEM
                || vowelType == VowelType.UU_STEM
                || vowelType == VowelType.R_STEM
                || vowelType == VowelType.PRON_AHAM
                || vowelType == VowelType.PRON_TVAM);

        if (isUnspecifiedGenderType) {
            var endings = caseEndingRepository.findByVowelTypeAndGenderAndCaseTypeAndNumberType(
                    vowelType, Gender.UNSPECIFIED, caseType, numberType);
            if (!endings.isEmpty()) {
                return endings.get(0);
            }
        }

        var endings = caseEndingRepository.findByVowelTypeAndGenderAndCaseTypeAndNumberType(
                vowelType, gender, caseType, numberType);
        if (!endings.isEmpty()) {
            return endings.get(0);
        }

        log.warn("Case ending not found for vowelType={}, gender={}, caseType={}, numberType={}",
                vowelType, gender, caseType, numberType);
        return null;
    }
}
