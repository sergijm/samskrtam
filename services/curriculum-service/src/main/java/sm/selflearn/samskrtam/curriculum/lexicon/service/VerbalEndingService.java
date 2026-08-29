package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.dto.VerbalEndingDto;
import sm.selflearn.samskrtam.curriculum.lexicon.lingua.VerbalEnding;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.VerbalEndingRepository;
import sm.selflearn.samskrtam.curriculum.mapper.VerbalEndingMapper;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VerbalEndingService {

    private final VerbalEndingRepository repository;
    private final VerbalEndingMapper mapper;

    @Transactional(readOnly = true)
    public List<VerbalEndingDto> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator
                        .comparing((VerbalEnding v) -> v.getTenseMood())
                        .thenComparing(v -> v.getPada())
                        .thenComparing(v -> v.getPersonNumber())
                        .thenComparing(v -> v.getEnding()))
                .map(mapper::toDto)
                .toList();
    }
}