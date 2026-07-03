package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.time.Instant;
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

    /**
     * Get a work by UUID string or slug. Tries UUID parsing first,
     * falls back to slug lookup.
     */
    @Transactional(readOnly = true)
    public Work getWorkByIdOrSlug(String idOrSlug) {
        try {
            return getWorkById(UUID.fromString(idOrSlug));
        } catch (IllegalArgumentException e) {
            return getWorkBySlug(idOrSlug);
        }
    }

    @Transactional
    public Work createWork(Work work) {
        if (workRepository.existsBySlug(work.getSlug())) {
            throw new RuntimeException("Work with slug '" + work.getSlug() + "' already exists");
        }
        work.setCreatedAt(Instant.now());
        return workRepository.save(work);
    }

    @Transactional
    public Work updateWork(UUID id, Work update) {
        Work work = getWorkById(id);
        work.setTitleRu(update.getTitleRu());
        work.setTitleEn(update.getTitleEn());
        work.setDescriptionRu(update.getDescriptionRu());
        work.setDescriptionEn(update.getDescriptionEn());
        work.setAuthor(update.getAuthor());
        return workRepository.save(work);
    }

    @Transactional
    public Work updateWorkBySlug(String slug, Work update) {
        Work work = getWorkBySlug(slug);
        work.setTitleRu(update.getTitleRu());
        work.setTitleEn(update.getTitleEn());
        work.setDescriptionRu(update.getDescriptionRu());
        work.setDescriptionEn(update.getDescriptionEn());
        work.setAuthor(update.getAuthor());
        return workRepository.save(work);
    }

    @Transactional
    public void deleteWork(UUID id) {
        Work work = getWorkById(id);
        work.setDeletedAt(Instant.now());
        workRepository.save(work);
    }

    @Transactional
    public void deleteWorkBySlug(String slug) {
        Work work = getWorkBySlug(slug);
        work.setDeletedAt(Instant.now());
        workRepository.save(work);
    }
}