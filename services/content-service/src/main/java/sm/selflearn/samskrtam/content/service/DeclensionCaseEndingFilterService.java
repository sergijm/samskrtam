package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.content.model.CaseEnding;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.content.repository.CaseEndingRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Фильтрует case_endings для урока склонений по опциональным фильтрам.
 * <p>
 * Для основ -i, -u, -ṛ окончания не различаются по роду — в БД gender = UNSPECIFIED.
 */
@Component
@RequiredArgsConstructor
public class DeclensionCaseEndingFilterService {

    private final CaseEndingRepository caseEndingRepository;

    public List<CaseEnding> filter(
            List<VowelType> vowelTypes,
            CaseType caseType,
            NumberType numberType,
            Gender gender) {
        boolean isUnspecifiedGenderType = SlugToVowelTypeMapper.isUnspecifiedGenderType(vowelTypes);

        List<CaseEnding> allEndings;

        if (caseType != null && numberType != null && gender != null && gender != Gender.UNSPECIFIED) {
            // Детальный фильтр (CASE_NUMBER_GENDER)
            if (isUnspecifiedGenderType) {
                // Для типов без родового различия — ищем UNSPECIFIED
                allEndings = findByVowelTypes(vowelTypes).stream()
                        .filter(ce -> ce.getGender() == Gender.UNSPECIFIED
                                && ce.getCaseType() == caseType
                                && ce.getNumberType() == numberType)
                        .collect(Collectors.toList());
                if (allEndings.isEmpty()) {
                    allEndings = findByVowelTypes(vowelTypes).stream()
                            .filter(ce -> ce.getGender() == gender
                                    && ce.getCaseType() == caseType
                                    && ce.getNumberType() == numberType)
                            .collect(Collectors.toList());
                }
            } else {
                allEndings = findByVowelTypes(vowelTypes).stream()
                        .filter(ce -> ce.getGender() == gender
                                && ce.getCaseType() == caseType
                                && ce.getNumberType() == numberType)
                        .collect(Collectors.toList());
            }
        } else if (caseType != null) {
            // Фильтр только по падежу (CASE_ONLY) — все числа и роды
            allEndings = findByVowelTypes(vowelTypes).stream()
                    .filter(ce -> ce.getCaseType() == caseType)
                    .collect(Collectors.toList());
        } else {
            // Без фильтра — все case_endings для этих vowelTypes
            allEndings = findByVowelTypes(vowelTypes);
        }

        return allEndings;
    }

    private List<CaseEnding> findByVowelTypes(List<VowelType> vowelTypes) {
        return caseEndingRepository.findByVowelTypeIn(vowelTypes);
    }
}
