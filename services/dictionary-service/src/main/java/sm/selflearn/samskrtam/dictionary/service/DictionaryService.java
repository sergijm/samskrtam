package sm.selflearn.samskrtam.dictionary.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.dictionary.dto.MwEntryDto;
import sm.selflearn.samskrtam.monierwilliams.dto.MwWordSearchDto;

import sm.selflearn.samskrtam.monierwilliams.dto.MwDictionaryEntryDto;
import sm.selflearn.samskrtam.monierwilliams.model.SanskritWordSearchResult;
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

    private final MwSanskritWordRepository mwSanskritWordRepository;
    private final MwEntryRepository mwEntryRepository;
    private final TransliterationService transliterationService;
    private final MwDictionaryEntryService mwDictionaryEntryService;

    public List<MwWordSearchDto> searchWords(String query) {
        String normalizedQuery = transliterationService.slp1RemoveStess(query);

        LevenshteinDistance ld = new LevenshteinDistance();

        return mwSanskritWordRepository.findBySlp1NormalizedSimilarity(normalizedQuery).stream()
                .map(this::mapToMwWordSearchDto)
                .collect(Collectors.groupingBy(MwWordSearchDto::getSlp1Normalized))
                .values().stream().map(list->
                    list.stream().max(Comparator.comparingDouble(MwWordSearchDto::getSimilarity)).orElseThrow()

                ).sorted(Comparator.comparingDouble(e->ld.apply(e.getSlp1Normalized(), query)))
                .collect(Collectors.toList());
    }

    public MwEntryDto getEntryBySlp1Spelling(String slp1Spelling) {

        List<MwDictionaryEntryDto> entries = mwDictionaryEntryService.getEntriesByKey1(slp1Spelling);
        return MwEntryDto.builder()
                .entries(entries)
                .build();
    }

    private MwWordSearchDto mapToMwWordSearchDto(SanskritWordSearchResult mwSanskritWord) {
        // Fetch the main headword (key1) from the associated MwEntry

        return MwWordSearchDto.builder()
                .slp1Spelling(mwSanskritWord.getSlp1Spelling())
                .slp1Normalized(mwSanskritWord.getSlp1Normalized())
                .iastSpelling(mwSanskritWord.getIastSpelling())
                .similarity(mwSanskritWord.getSimilarity())
                .build();
    }

}
