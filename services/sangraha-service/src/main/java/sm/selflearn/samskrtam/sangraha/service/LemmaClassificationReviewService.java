package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.LemmaClassificationItemDto;
import sm.selflearn.samskrtam.sangraha.dto.LemmaClassificationPageDto;
import sm.selflearn.samskrtam.sangraha.dto.LemmaClassificationReviewRequest;
import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.model.LemmaClassification;
import sm.selflearn.samskrtam.sangraha.model.LemmaStatistics;
import sm.selflearn.samskrtam.sangraha.repository.LemmaClassificationRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaStatisticsRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Admin review + экспорт APPROVED-классификаций (lemma-classification.md §4–§5,
 * task-sangraha-19). Статистика (occurrenceCount/dominantPosCode) берётся из
 * строки (lemma, gender) — той же пары, что у classification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaClassificationReviewService {

    private final LemmaClassificationRepository classificationRepository;
    private final LemmaStatisticsRepository statisticsRepository;
    private final LemmaClassificationValidator validator;

    /**
     * Список на ревью: status + schemeCode фильтры, пагинация курсором по
     * lemmaId, сортировка по occurrenceCount статистики (частотные приоритетнее, §4).
     */
    @Transactional(readOnly = true)
    public LemmaClassificationPageDto listForReview(String schemeCode, ClassificationStatus status, UUID cursor, int limit) {
        String code = schemeCode == null ? "CURRICULUM" : schemeCode;
        ClassificationStatus st = status == null ? ClassificationStatus.CANDIDATE : status;
        int limit1 = safeLimit(limit);
        List<LemmaClassification> rows = classificationRepository.findForReview(
                code, st, cursor, PageRequest.of(0, limit1));
        return toPage(rows);
    }

    /**
     * PATCH /classifications/{id}: Admin исправляет поля и/или статус.
     */
    @Transactional
    public LemmaClassificationItemDto review(UUID id, String schemeCode, LemmaClassificationReviewRequest request,
                                             String reviewedBy) {
        String code = schemeCode == null ? "CURRICULUM" : schemeCode;
        LemmaClassification row = classificationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Classification not found: " + id));
        if (!code.equals(row.getSchemeCode())) {
            throw new IllegalArgumentException("Classification belongs to scheme " + row.getSchemeCode());
        }

        if (request.categoryCode() != null) {
            if (!validator.isValidCategoryCode(request.categoryCode())) {
                throw new IllegalArgumentException("Unknown categoryCode: " + request.categoryCode());
            }
            row.setCategoryCode(request.categoryCode());
        }
        if (request.glossRu() != null) {
            row.setGlossRu(request.glossRu());
        }
        if (request.glossEn() != null) {
            row.setGlossEn(request.glossEn());
        }
        if (request.confidence() != null) {
            row.setConfidence(request.confidence());
        }
        if (request.status() != null) {
            ClassificationStatus st = ClassificationStatus.valueOf(request.status().toUpperCase());
            if (st == ClassificationStatus.APPROVED && !validator.isValidCategoryCode(row.getCategoryCode())) {
                throw new IllegalArgumentException("APPROVED requires a valid categoryCode");
            }
            row.setStatus(st);
            row.setReviewedBy(reviewedBy);
            row.setReviewedAt(Instant.now());
        }
        LemmaClassification saved = classificationRepository.save(row);
        return toDto(saved, statsFor(saved));
    }

    /**
     * Экспорт APPROVED в curriculum-service (lemma-classification.md §5): JOIN
     * Classification + Lemma + Statistics, курсор по lemmaId, сортировка
     * occurrenceCount статистики.
     */
    @Transactional(readOnly = true)
    public LemmaClassificationPageDto exportApproved(String schemeCode, UUID cursor, int limit) {
        String code = schemeCode == null ? "CURRICULUM" : schemeCode;
        int limit1 = safeLimit(limit);
        List<LemmaClassification> rows = classificationRepository.findForReview(
                code, ClassificationStatus.APPROVED, cursor, PageRequest.of(0, limit1));
        return toPage(rows);
    }

    private LemmaClassificationPageDto toPage(List<LemmaClassification> rows) {
        Map<UUID, LemmaStatistics> statsByPair = statsIndex(rows);
        List<LemmaClassificationItemDto> items = new ArrayList<>(rows.size());
        for (LemmaClassification row : rows) {
            items.add(toDto(row, statsByPair.get(pairKey(row))));
        }
        UUID nextCursor = rows.isEmpty() ? null : rows.get(rows.size() - 1).getLemma().getId();
        return new LemmaClassificationPageDto(items, nextCursor);
    }

    /** Индекс статистики по ключу (lemmaId, gender) для списка строк. */
    private Map<UUID, LemmaStatistics> statsIndex(List<LemmaClassification> rows) {
        List<UUID> lemmaIds = rows.stream().map(r -> r.getLemma().getId()).distinct().toList();
        return statisticsRepository.findByLemmaIdIn(lemmaIds).stream()
                .collect(Collectors.toMap(
                        s -> pairKey(s.getLemma().getId(), s.getGender()),
                        Function.identity(),
                        (a, b) -> a));
    }

    private LemmaStatistics statsFor(LemmaClassification row) {
        return statisticsRepository
                .findByLemmaIdAndGender(row.getLemma().getId(), row.getGender())
                .orElse(null);
    }

    private LemmaClassificationItemDto toDto(LemmaClassification row, LemmaStatistics stats) {
        Lemma lemma = row.getLemma();
        return new LemmaClassificationItemDto(
                row.getId(),
                lemma.getId(),
                lemma.getLemmaSlp1(),
                lemma.getLemmaIast(),
                lemma.getLemmaDevanagari(),
                row.getGender(),
                stats == null ? null : stats.getDominantPosCode(),
                stats == null ? null : stats.getOccurrenceCount(),
                row.getCategoryCode(),
                row.getGlossRu(),
                row.getGlossEn(),
                row.getConfidence(),
                row.getStatus() == null ? null : row.getStatus().name(),
                row.getReviewedBy(),
                row.getReviewedAt());
    }

    private int safeLimit(int limit) {
        return Math.max(1, Math.min(limit, 500));
    }

    private static UUID pairKey(LemmaClassification row) {
        return pairKey(row.getLemma().getId(), row.getGender());
    }

    private static UUID pairKey(UUID lemmaId, String gender) {
        return UUID.nameUUIDFromBytes((lemmaId + "|" + gender).getBytes());
    }
}