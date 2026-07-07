package sm.selflearn.samskrtam.monierwilliams.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.dictionary.service.TransliterationService;
import sm.selflearn.samskrtam.monierwilliams.dto.MwWordSearchDto;
import sm.selflearn.samskrtam.monierwilliams.dto.MwDictionaryEntryDto;
import sm.selflearn.samskrtam.monierwilliams.entity.MwEntry;
import sm.selflearn.samskrtam.monierwilliams.model.SanskritWordSearchResult;
import sm.selflearn.samskrtam.monierwilliams.repository.MwEntryRepository;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MwDictionaryEntryService {

    private final MwEntryRepository entryRepository;
    private final TransliterationService transliterationService;
    private final MwEntryBuilder entryBuilder;
    private final MwDictionaryEntryMapper mapper;

    /**
     * Получить полную статью по record_id
     */
    @Transactional(readOnly = true)
    public MwDictionaryEntryDto getEntryByRecordId(String recordId) {
        MwEntry entry = entryRepository.findByRecordIdFull(recordId)
                .orElseThrow(() -> new RuntimeException("Entry not found: " + recordId));
        return entryBuilder.build(entry);
    }

    /**
     * Получить список слов по ключу поиска (неточное совпадение)
     */
    public List<MwWordSearchDto> findWordsByKey1Normalized(String normalizedQuery) {
        List<SanskritWordSearchResult> words = entryRepository.findWordsByKey1NormalizedSimilarity(normalizedQuery);
        return words.stream().map(mapper::mapToMwWordSearchDto)
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Получить статью по ключу поиска (точное совпадение)
     */
    @Transactional(readOnly = true)
    public List<MwDictionaryEntryDto> getEntriesByKey1(String key1) {
        String slp1RemoveStress = transliterationService.slp1RemoveStress(key1);

        List<MwEntry> entries = entryRepository.findByKey1Normalized(slp1RemoveStress);

        List<MwEntry> sorted = entries.stream()
                .sorted(Comparator.comparing(MwEntry::getECode))
                .collect(Collectors.toUnmodifiableList());

        return sorted.stream()
                .map(entryBuilder::build)
                .collect(Collectors.toList());
    }

}

