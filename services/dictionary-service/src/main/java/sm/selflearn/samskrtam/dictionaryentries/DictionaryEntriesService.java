package sm.selflearn.samskrtam.dictionaryentries;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.apte.service.ApteService;
import sm.selflearn.samskrtam.cae.service.CaeService;
import sm.selflearn.samskrtam.frisch.service.FrischService;
import sm.selflearn.samskrtam.mw.service.MwService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DictionaryEntriesService {

    private final ApteService apteService;
    private final CaeService caeService;
    private final FrischService frischService;
    private final MwService mwService;

    public DictionaryEntriesResponse getEntries(String dictionary, List<Long> entryIds) {
        if (entryIds == null || entryIds.isEmpty()) {
            return DictionaryEntriesResponse.builder()
                    .dictionary(dictionary)
                    .entries(List.of())
                    .build();
        }
        List<?> entries = switch (dictionary.toLowerCase()) {
            case "apte" -> apteService.getEntriesByIds(entryIds);
            case "cae" -> caeService.getEntriesByIds(entryIds);
            case "frisch" -> frischService.getEntriesByIds(entryIds);
            case "mw" -> mwService.getEntriesByIds(entryIds);
            default -> throw new IllegalArgumentException("Unknown dictionary: " + dictionary);
        };
        return DictionaryEntriesResponse.builder()
                .dictionary(dictionary)
                .entries((List<Object>) entries)
                .build();
    }
}
