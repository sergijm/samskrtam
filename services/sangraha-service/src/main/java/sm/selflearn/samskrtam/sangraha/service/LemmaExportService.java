package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.sangraha.dto.LemmaExportItemDto;
import sm.selflearn.samskrtam.sangraha.dto.LemmaExportPageDto;
import sm.selflearn.samskrtam.sangraha.model.LemmaClassification;
import sm.selflearn.samskrtam.sangraha.model.LemmaStatistics;
import sm.selflearn.samskrtam.sangraha.model.NominalLemma;
import sm.selflearn.samskrtam.sangraha.repository.LemmaClassificationRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaStatisticsRepository;
import sm.selflearn.samskrtam.sangraha.repository.NominalLemmaRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Экспорт уникальных пар (лемма, род) из {@code lemma_statistics}, отсортированных
 * по частоте вхождения по убыванию, для batch-импорта в curriculum-service.
 *
 * Каждая строка — {@code LemmaStatistics} (UNIQUE(lemma_id, gender)): частота
 * (occurrenceCount), доминирующая часть речи (dominantPosCode) и род; к ней
 * присоединяются классификация (categoryCode, gloss) по схеме CURRICULUM и класс
 * основы (vowelType) из nominal_lemmas. Классификация опциональна — строки без
 * неё экспортируются с пустыми categoryCodes/gloss.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaExportService {

    private final LemmaStatisticsRepository statisticsRepository;
    private final LemmaClassificationRepository classificationRepository;
    private final NominalLemmaRepository nominalLemmaRepository;

    @Transactional(readOnly = true)
    public LemmaExportPageDto export(UUID cursor, int limit) {
        int pageSize = Math.min(Math.max(limit, 1), 5000);

        List<LemmaStatistics> stats = statisticsRepository.findForExport(
                cursor, PageRequest.of(0, pageSize + 1));

        if (stats.isEmpty()) {
            return new LemmaExportPageDto(List.of(), null);
        }

        boolean hasMore = stats.size() > pageSize;
        if (hasMore) {
            stats = stats.subList(0, pageSize);
        }

        List<UUID> lemmaIds = stats.stream()
                .map(s -> s.getLemma().getId())
                .distinct()
                .toList();
        List<String> lemmaIasts = stats.stream()
                .map(s -> s.getLemma().getLemmaIast())
                .distinct()
                .toList();

        Map<String, LemmaClassification> classificationByKey = loadClassifications(lemmaIds);
        Map<String, List<String>> categoryCodesByKey = loadCategoryCodes(lemmaIds);
        Map<String, String> vowelTypeByIast = loadVowelTypes(lemmaIasts);

        List<LemmaExportItemDto> items = new ArrayList<>();
        for (LemmaStatistics s : stats) {
            String gender = s.getGender();
            String key = s.getLemma().getId() + "\0" + (gender == null ? "" : gender);
            LemmaClassification classification = classificationByKey.get(key);
            List<String> categoryCodes = categoryCodesByKey.getOrDefault(key, List.of());
            String vowelType = vowelTypeByIast.get(s.getLemma().getLemmaIast());

            items.add(new LemmaExportItemDto(
                    s.getLemma().getId(),
                    s.getLemma().getLemmaSlp1(),
                    s.getLemma().getLemmaIast(),
                    s.getLemma().getLemmaDevanagari(),
                    gender,
                    s.getDominantPosCode(),
                    s.getOccurrenceCount(),
                    categoryCodes,
                    classification == null ? null : classification.getGlossRu(),
                    classification == null ? null : classification.getGlossEn(),
                    vowelType
            ));
        }

        UUID nextCursor = hasMore ? stats.get(stats.size() - 1).getId() : null;
        return new LemmaExportPageDto(items, nextCursor);
    }

    private Map<String, LemmaClassification> loadClassifications(List<UUID> lemmaIds) {
        List<LemmaClassification> rows = classificationRepository.findBySchemeCodeAndLemmaIdIn(
                "CURRICULUM", lemmaIds);
        Map<String, LemmaClassification> result = new HashMap<>();
        for (LemmaClassification lc : rows) {
            String gender = lc.getGender() == null ? "" : lc.getGender();
            result.put(lc.getLemma().getId() + "\0" + gender, lc);
        }
        return result;
    }

    private Map<String, List<String>> loadCategoryCodes(List<UUID> lemmaIds) {
        List<LemmaClassification> rows = classificationRepository.findBySchemeCodeAndLemmaIdIn(
                "CURRICULUM", lemmaIds);
        Map<String, List<String>> result = new HashMap<>();
        for (LemmaClassification lc : rows) {
            if (lc.getCategoryCode() == null) {
                continue;
            }
            String gender = lc.getGender() == null ? "" : lc.getGender();
            String key = lc.getLemma().getId() + "\0" + gender;
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(lc.getCategoryCode());
        }
        return result;
    }

    private Map<String, String> loadVowelTypes(List<String> lemmaIasts) {
        List<NominalLemma> all = nominalLemmaRepository.findByLemmaIastIn(lemmaIasts);
        Map<String, String> result = new HashMap<>();
        for (NominalLemma nl : all) {
            if (nl.getStemClass() != null) {
                try {
                    VowelType.valueOf(nl.getStemClass());
                    result.put(nl.getLemmaIast(), nl.getStemClass());
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return result;
    }
}
