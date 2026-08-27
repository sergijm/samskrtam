package sm.selflearn.samskrtam.frisch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.dto.frisch.FrischEntryDto;
import sm.selflearn.samskrtam.frisch.repository.FrischRepository;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FrischService {

    private final FrischRepository frischRepository;

    public List<FrischEntryDto> getLemma(String lemma) {
        return frischRepository.getLemmaJson(lemma);
    }

    public List<FrischEntryDto> getEntriesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Integer> intIds = ids.stream().map(Long::intValue).toList();
        List<String> lemmas = frischRepository.findLemmaIastsByEntryIds(intIds);
        List<FrischEntryDto> result = new ArrayList<>();
        for (String lemma : lemmas) {
            result.addAll(frischRepository.getLemmaJson(lemma));
        }
        return result;
    }
}
