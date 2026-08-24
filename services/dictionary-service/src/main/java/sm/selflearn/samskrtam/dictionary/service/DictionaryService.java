package sm.selflearn.samskrtam.dictionary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.dictionary.dto.MwEntryDto;
import sm.selflearn.samskrtam.monierwilliams.dto.MwDictionaryEntryDto;
import sm.selflearn.samskrtam.monierwilliams.service.MwDictionaryEntryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final MwDictionaryEntryService mwDictionaryEntryService;

    /**
     * Простой поиск по лемме в IAST. Без предварительного нечёткого поиска —
     * запрос сопоставляется с колонкой key1_iast_plain напрямую в БД.
     */
    public MwEntryDto searchByLemma(String query) {
        List<MwDictionaryEntryDto> entries = mwDictionaryEntryService.getEntriesByLemmaIast(query);
        return MwEntryDto.builder()
                .entries(entries)
                .build();
    }

    public MwEntryDto getEntryBySlp1Spelling(String slp1Spelling) {
        List<MwDictionaryEntryDto> entries = mwDictionaryEntryService.getEntriesByKey1(slp1Spelling);
        return MwEntryDto.builder()
                .entries(entries)
                .build();
    }

}
