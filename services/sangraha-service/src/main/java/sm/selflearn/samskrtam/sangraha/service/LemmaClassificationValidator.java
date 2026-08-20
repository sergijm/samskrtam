package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.repository.CurriculumSemanticClassRepository;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Валидация tool-ответа классификации (lemma-classification.md §2.3–§2.4,
 * task-sangraha-18 шаги 9–10).
 */
@Component
@RequiredArgsConstructor
public class LemmaClassificationValidator {

    private static final String DEVANAGARI_REGEX = "\\p{InDevanagari}";

    private final CurriculumSemanticClassRepository topicRepository;

    /**
     * Проверяет, что {@code categoryCode} есть в справочнике CURRICULUM.
     */
    public boolean isValidCategoryCode(String categoryCode) {
        if (categoryCode == null || categoryCode.isBlank()) {
            return false;
        }
        return validCodes().contains(categoryCode);
    }

    /**
     * Транслитерации не должны содержать деванагари (признак ошибки модели, §2.4).
     */
    public boolean containsDevanagari(String text) {
        return text != null && text.matches(".*" + DEVANAGARI_REGEX + ".*");
    }

    private Set<String> validCodes() {
        return topicRepository.findAll().stream()
                .map(t -> t.getCode())
                .collect(Collectors.toSet());
    }
}