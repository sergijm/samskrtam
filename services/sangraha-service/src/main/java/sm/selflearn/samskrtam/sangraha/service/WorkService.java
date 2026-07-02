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
    public void deleteWork(UUID id) {
        Work work = getWorkById(id);
        work.setDeletedAt(Instant.now());
        workRepository.save(work);
    }
}