package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.StandaloneVerseItemDto;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Standalone-стихи пользователя (страница /analysis): verse.chapter_id = null,
 * стих не привязан к произведению/главе. Каждое нажатие «Анализировать»
 * создаёт новую запись в verses и запускает LLM-анализ (createAndAnalyze).
 */
@Service
@RequiredArgsConstructor
public class StandaloneVerseService {

    private final VerseRepository verseRepository;
    private final VerseAnalysisService verseAnalysisService;

    /**
     * Создаёт standalone-стих с текстом пользователя и сразу запускает анализ.
     * order_index для standalone-стихов не осмыслен (нет главы) — выставляем 0.
     *
     * @return созданный стих (статус ANALYZING сразу после запуска анализа)
     *
     * <p>Без @Transactional: LLM-анализ через {@link VerseAnalysisService#analyze}
     * не должен удерживать транзакцию на время вызова LLM (как и в штатном
     * POST /verses/{verseId}/analyze). Сохранение стиха — отдельная короткая
     * транзакция репозитория; при сбое анализа стих останется в DRAFT/FAILED.</p>
     */
    public Verse createAndAnalyze(String text, UUID ownerId) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Text must not be blank");
        }

        Instant now = Instant.now();
        Verse verse = Verse.builder()
                .chapterId(null)
                .ownerId(ownerId)
                .orderIndex(0)
                .rawText(text)
                .status(VerseStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();
        verse = verseRepository.save(verse);

        verseAnalysisService.analyze(verse.getId(), text);
        return verse;
    }

    /** Список standalone-стихов пользователя, новые сверху. */
    @Transactional(readOnly = true)
    public List<StandaloneVerseItemDto> list(UUID ownerId) {
        return verseRepository
                .findAllByChapterIdIsNullAndOwnerIdAndDeletedAtIsNullOrderByCreatedAtDesc(ownerId)
                .stream()
                .map(v -> new StandaloneVerseItemDto(
                        v.getId(),
                        ChapterService.preview(previewText(v), 120),
                        v.getStatus(),
                        v.getCreatedAt()))
                .toList();
    }

    /** Мягкое удаление standalone-стиха владельцем. */
    @Transactional
    public void delete(UUID verseId, UUID ownerId) {
        Verse verse = verseRepository.findByIdAndDeletedAtIsNull(verseId)
                .orElseThrow(() -> new IllegalArgumentException("Verse not found: " + verseId));
        if (verse.getChapterId() != null) {
            throw new IllegalArgumentException("Verse is not standalone: " + verseId);
        }
        if (!ownerId.equals(verse.getOwnerId())) {
            throw new IllegalArgumentException("Verse does not belong to user: " + verseId);
        }
        verse.setDeletedAt(Instant.now());
        verse.setUpdatedAt(Instant.now());
        verseRepository.save(verse);
    }

    private static String previewText(Verse v) {
        if (v.getTextIast() != null && !v.getTextIast().isBlank()) {
            return v.getTextIast();
        }
        if (v.getTextDevanagari() != null && !v.getTextDevanagari().isBlank()) {
            return v.getTextDevanagari();
        }
        return v.getRawText();
    }
}
