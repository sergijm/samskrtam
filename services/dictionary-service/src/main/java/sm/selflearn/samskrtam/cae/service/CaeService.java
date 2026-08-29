package sm.selflearn.samskrtam.cae.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.cae.dto.CaeEntryDto;
import sm.selflearn.samskrtam.cae.repository.CaeRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CaeService {

    private final CaeRepository caeRepository;

    public List<CaeEntryDto> getEntriesByIds(List<Long> ids) {
        return caeRepository.findByIds(ids);
    }
}
