package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.model.Source;
import sm.selflearn.samskrtam.sangraha.repository.SourceRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SourceService {

    private final SourceRepository sourceRepository;

    @Transactional(readOnly = true)
    public List<Source> getAllSources() {
        return sourceRepository.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public Optional<Source> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        return sourceRepository.findByCode(code);
    }
}
