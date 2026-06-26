package sm.selflearn.samskrtam.dictionary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.dictionary.dto.MwEntryDto;
import sm.selflearn.samskrtam.dictionary.provider.DictionaryProvider;
import sm.selflearn.samskrtam.monierwilliams.dto.MwWordSearchDto;

import sm.selflearn.samskrtam.monierwilliams.dto.MwDictionaryEntryDto;
import sm.selflearn.samskrtam.monierwilliams.repository.MwEntryRepository;
import sm.selflearn.samskrtam.monierwilliams.repository.MwSanskritWordRepository;
import org.apache.commons.text.similarity.LevenshteinDistance;
import sm.selflearn.samskrtam.monierwilliams.service.MwDictionaryEntryService;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final MwEntryRepository mwEntryRepository;
    private final TransliterationService transliterationService;
    private final MwDictionaryEntryService mwDictionaryEntryService;
    private final List<DictionaryProvider> providers;

    public List<MwWordSearchDto> searchWords(String query) {
        String normalizedQuery = transliterationService.normalizeToSlp1(query,null);

        LevenshteinDistance ld = new LevenshteinDistance();

        return mwDictionaryEntryService.findWordsByKey1Normalized(normalizedQuery).stream()
                .collect(Collectors.groupingBy(MwWordSearchDto::getSlp1Normalized))
                .values().stream().map(list->
                    list.stream().max(Comparator.comparingDouble(MwWordSearchDto::getSimilarity)).orElseThrow()

                ).sorted(Comparator.comparingDouble(e->ld.apply(e.getSlp1Normalized(), query)))
                .limit(30)
                .collect(Collectors.toList());
    }

    public MwEntryDto getEntryBySlp1Spelling(String slp1Spelling) {

        List<MwDictionaryEntryDto> entries = mwDictionaryEntryService.getEntriesByKey1(slp1Spelling);
        return MwEntryDto.builder()
                .entries(entries)
                .build();
    }

}

