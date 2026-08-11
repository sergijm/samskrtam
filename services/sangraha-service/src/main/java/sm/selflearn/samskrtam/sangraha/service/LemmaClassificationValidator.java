package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.repository.CurriculumSemanticTopicRepository;

@Component
@RequiredArgsConstructor
public class LemmaClassificationValidator {

    private static final String DEVANAGARI_REGEX = "\\p{InDevanagari}";

    private final CurriculumSemanticTopicRepository topicRepository;

    public boolean isValidCategoryCode(String categoryCode) {
        return categoryCode != null && !categoryCode.isBlank() && topicRepository.existsById(categoryCode);
    }

    public boolean containsDevanagari(String text) {
        return text != null && text.matches(".*" + DEVANAGARI_REGEX + ".*");
    }
}