package sm.selflearn.samskrtam.apte.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.apte.dto.ApteEntryDto;
import sm.selflearn.samskrtam.apte.repository.ApteRepository;
import sm.selflearn.samskrtam.dictionary.service.TransliterationService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApteService {

    private final ApteRepository apteRepository;
    private final TransliterationService transliterationService;

    /**
     * Поиск словарной статьи Apte по лемме. Вход — IAST (или devanagari);
     * транслитерируется в SLP1 и матчится с колонкой k1_slp1.
     */
    public List<ApteEntryDto> getEntriesByLemma(String lemma) {
        String slp1 = transliterationService.slp1RemoveStress(
                transliterationService.normalizeToSlp1(lemma, null));
        return apteRepository.findByK1Slp1(slp1);
    }

    public List<ApteEntryDto> getEntriesByIds(List<Long> ids) {
        return apteRepository.findByIds(ids);
    }
}
