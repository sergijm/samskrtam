package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.dto.CaseEndingDto;
import sm.selflearn.samskrtam.curriculum.lexicon.lingua.CaseEnding;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.CaseEndingRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.mapper.CaseEndingMapper;
import sm.selflearn.samskrtam.curriculum.questgen.morphology.CaseType;
import sm.selflearn.samskrtam.morphology.NumberType;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CaseEndingService {

    private final CaseEndingRepository repository;
    private final CaseEndingMapper mapper;

    @Transactional(readOnly = true)
    public List<CaseEndingDto> findAll() {
        return repository.findAll().stream()
                .sorted(Comparator
                        .comparing((CaseEnding c) -> c.getStemType().name())
                        .thenComparing(c -> c.getPos().name())
                        .thenComparing(c -> c.getGender().name(), Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(c -> c.getNumber().name())
                        .thenComparing(c -> c.getGrammaticalCase().name()))
                .map(mapper::toDto)
                .toList();
    }
}
