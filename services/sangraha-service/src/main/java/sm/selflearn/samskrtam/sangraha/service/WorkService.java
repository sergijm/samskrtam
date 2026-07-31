package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;

    @Transactional(readOnly = true)
    public List<Work> getAllWorks() {
        return workRepository.findAllByDeletedAtIsNullOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public Work getWorkById(UUID id) {
        return workRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Work not found: " + id));
    }

    @Transactional(readOnly = true)
    public Work getWorkBySlug(String slug) {
        return workRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new RuntimeException("Work not found by slug: " + slug));
    }

    @Transactional(readOnly = true)
    public Work getWorkByIdOrSlug(String idOrSlug) {
        try {
            return getWorkById(UUID.fromString(idOrSlug));
        } catch (IllegalArgumentException e) {
            return getWorkBySlug(idOrSlug);
        }
    }
}
