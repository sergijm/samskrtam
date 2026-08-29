package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.CreateWorkRequest;
import sm.selflearn.samskrtam.sangraha.dto.UpdateWorkRequest;
import sm.selflearn.samskrtam.sangraha.dto.WorkSummaryDto;
import sm.selflearn.samskrtam.sangraha.model.Source;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;
    private final SourceService sourceService;

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

    // ── Write: создание/обновление произведения (ADMIN, см. §4/§5.2) ──

    @Transactional
    public Work createWork(CreateWorkRequest req) {
        if (req.titleRu() == null || req.titleRu().isBlank()) {
            throw new IllegalArgumentException("titleRu must not be blank");
        }
        UUID sourceId = resolveSourceId(req.sourceCode());
        String slug = SlugUtils.uniqueSlug(req.titleRu(), workRepository::existsBySlug);
        Instant now = Instant.now();
        Work work = Work.builder()
                .slug(slug)
                .titleRu(req.titleRu())
                .titleEn(req.titleEn() == null ? req.titleRu() : req.titleEn())
                .titleSaIast(req.titleSaIast())
                .titleSaDevanagari(req.titleSaDevanagari())
                .sourceId(sourceId)
                .createdAt(now)
                .build();
        return workRepository.save(work);
    }

    @Transactional
    public Work updateWork(String workSlug, UpdateWorkRequest req) {
        Work work = getWorkBySlug(workSlug);
        if (req.titleRu() != null) {
            work.setTitleRu(req.titleRu());
        }
        if (req.titleEn() != null) {
            work.setTitleEn(req.titleEn());
        }
        if (req.titleSaIast() != null) {
            work.setTitleSaIast(req.titleSaIast());
        }
        if (req.titleSaDevanagari() != null) {
            work.setTitleSaDevanagari(req.titleSaDevanagari());
        }
        if (req.author() != null) {
            work.setAuthor(req.author());
        }
        return workRepository.save(work);
    }

    private UUID resolveSourceId(String sourceCode) {
        String code = (sourceCode != null && !sourceCode.isBlank()) ? sourceCode : DEFAULT_SOURCE_CODE;
        return sourceService.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Source not found: " + code))
                .getId();
    }

    /** Источник по умолчанию для произведений, созданных вручную через UI. */
    private static final String DEFAULT_SOURCE_CODE = "manual";

    public static WorkSummaryDto toSummary(Work w) {
        return new WorkSummaryDto(
                w.getId(),
                w.getSlug(),
                w.getTitleRu(),
                w.getTitleEn(),
                w.getTitleSaIast(),
                w.getTitleSaDevanagari(),
                w.getDescriptionRu(),
                w.getDescriptionEn(),
                w.getAuthor(),
                w.getCreatedAt(),
                0,
                0,
                0
        );
    }
}
