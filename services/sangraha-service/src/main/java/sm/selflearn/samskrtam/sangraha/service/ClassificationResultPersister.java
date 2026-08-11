package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.model.LemmaClassification;
import sm.selflearn.samskrtam.sangraha.repository.LemmaClassificationRepository;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationPromptBuilder.LemmaBatchItem;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сохраняет результаты LLM-классификации в отдельной транзакции (REQUIRES_NEW).
 * Даже при падении основной транзакции прогона результаты уже закоммичены.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassificationResultPersister {

    private final LemmaClassificationRepository classificationRepository;
    private final LemmaClassificationValidator validator;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int persist(List<LemmaClassificationSuggestion> suggestions,
                       List<LemmaBatchItem> items,
                       String schemeCode,
                       String llmModel) {
        if (suggestions == null || suggestions.isEmpty()) return 0;

        Map<UUID, LemmaBatchItem> byLemmaId = items.stream()
                .collect(Collectors.toMap(it -> it.lemma().getId(), Function.identity()));
        int saved = 0;

        for (LemmaClassificationSuggestion suggestion : suggestions) {
            LemmaBatchItem item = byLemmaId.get(suggestion.lemmaId());
            if (item == null) {
                log.warn("Model returned unknown lemmaId {}, skipping", suggestion.lemmaId());
                continue;
            }
            if (validator.containsDevanagari(suggestion.glossRu()) || validator.containsDevanagari(suggestion.glossEn())) {
                log.warn("Devanagari in gloss, skipping lemma {}", suggestion.lemmaId());
                continue;
            }
            String categoryCode = validator.isValidCategoryCode(suggestion.categoryCode())
                    ? suggestion.categoryCode() : null;
            LemmaClassification existing = classificationRepository
                    .findByLemmaIdAndGenderAndSchemeCode(suggestion.lemmaId(), item.gender(), schemeCode)
                    .orElse(null);
            if (existing == null) {
                existing = LemmaClassification.builder()
                        .lemma(item.lemma())
                        .gender(item.gender())
                        .schemeCode(schemeCode)
                        .status(ClassificationStatus.APPROVED)
                        .build();
            }
            existing.setCategoryCode(categoryCode);
            existing.setGlossRu(suggestion.glossRu());
            existing.setGlossEn(suggestion.glossEn());
            existing.setConfidence(suggestion.confidence());
            existing.setLlmModel(llmModel);
            classificationRepository.save(existing);
            saved++;
        }
        return saved;
    }
}