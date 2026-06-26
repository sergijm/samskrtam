package sm.selflearn.samskrtam.dictionary.provider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.dictionary.model.*;
import sm.selflearn.samskrtam.dictionary.service.TransliterationService;
import sm.selflearn.samskrtam.monierwilliams.service.MwDictionaryEntryService;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MwDictionaryProvider implements DictionaryProvider {
    private final MwDictionaryEntryService mwService;
    private final TransliterationService transliterationService;

    @Override
    public DictionarySource getSource() {
        return DictionarySource.MONIER_WILLIAMS;
    }

    @Override
    public List<WordSearchResult> search(String slp1Query) {
        return mwService.findWordsByKey1Normalized(slp1Query).stream()
                .map(dto -> WordSearchResult.builder()
                        .slp1Spelling(dto.getSlp1Spelling())
                        .slp1Normalized(dto.getSlp1Normalized())
                        .iastSpelling(dto.getIastSpelling())
                        .similarity(dto.getSimilarity())
                        .source(DictionarySource.MONIER_WILLIAMS)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<DictionaryEntry> getEntries(String slp1Key) {
        return mwService.getEntriesByKey1(slp1Key).stream()
                .map(dto -> DictionaryEntry.builder()
                        .key(slp1Key)
                        .keyIast(transliterationService.slp1ToIast(slp1Key))
                        .displayTitle(dto.getKey1Display())
                        .mainTranslation(dto.getMainTranslation())
                        .rawBody(dto.getRawBody())
                        .source(DictionarySource.MONIER_WILLIAMS)
                        .sourceSpecific(dto)
                        .build())
                .collect(Collectors.toList());
    }
}