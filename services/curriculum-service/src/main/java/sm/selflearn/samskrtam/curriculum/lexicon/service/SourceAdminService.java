package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.SourceUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Source;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SourceOccurrenceRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SourceRepository;

import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD источников корпуса + пересчёт кэшей occurrence (task-curriculum-16 §7).
 */
@Service
@RequiredArgsConstructor
public class SourceAdminService {

    private final SourceRepository sourceRepository;
    private final SourceOccurrenceRepository occurrenceRepository;

    @Transactional(readOnly = true)
    public List<Source> list() {
        return sourceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Source get(UUID id) {
        return sourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found"));
    }

    @Transactional
    public Source create(SourceUpsertRequest request) {
        Source source = new Source();
        apply(source, request);
        return sourceRepository.save(source);
    }

    @Transactional
    public Source update(UUID id, SourceUpsertRequest request) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found"));
        apply(source, request);
        return sourceRepository.save(source);
    }

    @Transactional
    public void delete(UUID id) {
        if (!sourceRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found");
        }
        sourceRepository.deleteById(id);
    }

    /**
     * Пересчёт totalOccurrencesCache/uniqueLemmaCountCache прямым COUNT (task §7).
     */
    @Transactional
    public Source refreshCache(UUID id) {
        Source source = sourceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Source not found"));
        source.setTotalOccurrencesCache((int) occurrenceRepository.countBySourceId(id));
        source.setUniqueLemmaCountCache((int) occurrenceRepository.countDistinctBySourceId(id));
        return sourceRepository.save(source);
    }

    private void apply(Source source, SourceUpsertRequest request) {
        source.setCode(request.code());
        source.setTitleRu(request.titleRu());
        source.setTitleEn(request.titleEn());
        source.setKind(request.kind());
        source.setExternalSangrahaWorkSlug(request.externalSangrahaWorkSlug());
    }
}