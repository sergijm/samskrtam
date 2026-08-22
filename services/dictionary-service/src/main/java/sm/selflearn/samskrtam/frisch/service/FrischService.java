package sm.selflearn.samskrtam.frisch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.dto.frisch.FrischEntryDto;
import sm.selflearn.samskrtam.frisch.repository.FrischRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FrischService {

    private final FrischRepository frischRepository;

    public List<FrischEntryDto> getLemma(String lemma) {
        return frischRepository.getLemmaJson(lemma);
    }
}
