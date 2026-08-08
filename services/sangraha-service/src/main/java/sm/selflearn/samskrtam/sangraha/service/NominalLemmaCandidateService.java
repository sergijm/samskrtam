package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.NominalLemmaCandidateDto;
import sm.selflearn.samskrtam.sangraha.dto.NominalLemmaCandidatesResponseDto;
import sm.selflearn.samskrtam.sangraha.model.Gender;
import sm.selflearn.samskrtam.sangraha.repository.NominalLemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.NominalLemmaRepository.CandidateRow;
import sm.selflearn.samskrtam.sangraha.repository.NominalLemmaRepository.LemmaGenderCount;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Возвращает кандидатов существительных для импорта склонений в
 * curriculum-service. Источник — {@code nominal_lemmas} (классификация стемов),
 * отсортированная по частоте в {@code verse_words}; при запросе по конкретному
 * классу основы возвращаются только леммы этого класса. Каждый кандидат
 * обогащается: родом (самый частый род вхождений в {@code verse_word_morphology})
 * и транслитерациями (devanagari/slp1).
 *
 * @see NominalLemmaRepository#findCandidatesByStemClass
 */
@Service
@RequiredArgsConstructor
public class NominalLemmaCandidateService {

    private final NominalLemmaRepository nominalLemmaRepository;
    private final TransliterationService transliterationService;

    /**
     * @param stemClass ограничение по классу основы (A_STEM … R_STEM), или
     *                  null/пусто — вернуть все классифицированные существительные
     * @param limit     максимальное число кандидатов (>= 0)
     */
    @Transactional(readOnly = true)
    public NominalLemmaCandidatesResponseDto findCandidates(String stemClass, int limit) {
        int safeLimit = Math.max(0, limit);
        PageRequest pageRequest = PageRequest.of(0, safeLimit);
        List<CandidateRow> rows =
                (stemClass == null || stemClass.isBlank())
                        ? nominalLemmaRepository.findAllCandidates(pageRequest)
                        : nominalLemmaRepository.findCandidatesByStemClass(stemClass, pageRequest);
        Map<String, Gender> genders = mostFrequentGenders(rows);
        List<NominalLemmaCandidateDto> candidates = rows.stream()
                .map(row -> new NominalLemmaCandidateDto(
                        row.getLemmaIast(),
                        transliterationService.iastToDevanagari(row.getLemmaIast()),
                        transliterationService.iastToSlp1(row.getLemmaIast()),
                        row.getStemIast(),
                        row.getStemClass(),
                        null,
                        genders.get(row.getLemmaIast()),
                        row.getFrequency()))
                .toList();
        return new NominalLemmaCandidatesResponseDto(candidates);
    }

    /**
     * Самый частый род леммы по {@code verse_word_morphology}. Леммы без
     * morphological-вхождений отсутствуют в результате.
     */
    private Map<String, Gender> mostFrequentGenders(List<CandidateRow> rows) {
        if (rows.isEmpty()) {
            return Map.of();
        }
        List<String> lemmas = rows.stream().map(CandidateRow::getLemmaIast).toList();
        Map<String, Gender> mostFrequent = new HashMap<>();
        Map<String, Long> best = new HashMap<>();
        for (LemmaGenderCount row : nominalLemmaRepository.countGenderByLemma(lemmas)) {
            Long prev = best.get(row.getLemmaIast());
            if (prev == null || prev < row.getCnt()) {
                best.put(row.getLemmaIast(), row.getCnt());
                mostFrequent.put(row.getLemmaIast(), row.getGender());
            }
        }
        return mostFrequent;
    }
}