package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.VerseBatchItemDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseBatchResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchResponseDto.VerseDto;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseAnalysis;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseAnalysisRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис для POST /sangraha/internal/content/verses/batch (sangraha-service.md §9).
 * Пакетная выдача стихов по ID: только ANALYZED, не удалённые, с тайтлами произведения и главы.
 * Не найденные / не-ANALYZED / удалённые стихи просто отсутствуют в ответе.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerseBatchService {

    private final VerseRepository verseRepository;
    private final VerseAnalysisRepository verseAnalysisRepository;
    private final ChapterRepository chapterRepository;
    private final WorkRepository workRepository;

    @Transactional(readOnly = true)
    public VersesBatchResponseDto fetchBatch(VersesBatchRequestDto request) {
        if (request == null || request.verseIds() == null || request.verseIds().isEmpty()) {
            return new VersesBatchResponseDto(List.of());
        }

        // Только ANALYZED и не удалённые
//        List<Verse> verses = verseRepository.findAllByIdInAndStatusAndDeletedAtIsNull(
//                request.verseIds(), VerseStatus.ANALYZED);

        List<Verse> verses = verseRepository.findAllByIdInAndDeletedAtIsNull(
                request.verseIds());

        if (verses.isEmpty()) {
            return new VersesBatchResponseDto(List.of());
        }

        // Группируем по chapterId для пакетной загрузки глав
        Map<UUID, Chapter> chapters = loadChapters(verses);
        Map<UUID, Work> works = loadWorks(chapters.values());

        // Загружаем анализы (переводы) для всех стихов сразу
        List<UUID> verseIds = verses.stream().map(Verse::getId).toList();
        Map<UUID, VerseAnalysis> analyses = verseAnalysisRepository.findAllByVerseIdIn(verseIds)
                .stream()
                .collect(Collectors.toMap(VerseAnalysis::getVerseId, Function.identity()));

        List<VerseDto> dtos = new ArrayList<>(verses.size());
        for (Verse verse : verses) {
            Chapter chapter = chapters.get(verse.getChapterId());
            if (chapter == null) continue;

            Work work = works.get(chapter.getWorkId());
            if (work == null) continue;

            VerseAnalysis analysis = analyses.get(verse.getId());

            dtos.add(new VerseDto(
                    verse.getId(),
                    work.getSlug(),
                    Optional.ofNullable(verse.getTextIast()).orElse(verse.getRawText()),
                    verse.getTextDevanagari(),
                    analysis == null ? null : analysis.getTranslationRu(),
                    analysis == null ? null : analysis.getTranslationEn(),
                    work.getTitleRu(),
                    work.getTitleEn(),
                    chapter.getTitleRu(),
                    chapter.getTitleEn(),
                    verse.getOrderIndex()
            ));
        }

        return new VersesBatchResponseDto(dtos);
    }

    /**
     * GET /api/v1/sangraha/verse (sangraha-service/batch-verse-review.md).
     * Пакетная выдача стихов по произвольному списку id с их {@code status}
     * (без фильтра по ANALYZED). Не найденные/удалённые id молча отсутствуют.
     * Порядок элементов — по порядку id в запросе.
     */
    @Transactional(readOnly = true)
    public VerseBatchResponseDto fetchBatchReview(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return new VerseBatchResponseDto(List.of());
        }

        List<Verse> verses = verseRepository.findAllByIdInAndDeletedAtIsNull(ids);
        if (verses.isEmpty()) {
            return new VerseBatchResponseDto(List.of());
        }

        Map<UUID, Verse> verseById = verses.stream()
                .collect(Collectors.toMap(Verse::getId, Function.identity()));

        Map<UUID, Chapter> chapters = loadChapters(verses);
        Map<UUID, Work> works = loadWorks(chapters.values());

        List<VerseBatchItemDto> dtos = new ArrayList<>();
        for (UUID id : ids) {
            Verse verse = verseById.get(id);
            if (verse == null) continue;

            Chapter chapter = chapters.get(verse.getChapterId());
            if (chapter == null) continue;

            Work work = works.get(chapter.getWorkId());
            if (work == null) continue;

            // Превью: textIast, а если его нет (например DRAFT) — fallback на rawText.
            String previewText = Optional.ofNullable(verse.getTextIast())
                    .filter(s -> !s.isBlank())
                    .orElse(verse.getRawText());

            dtos.add(new VerseBatchItemDto(
                    id,
                    work.getSlug(),
                    work.getTitleRu(),
                    work.getTitleEn(),
                    chapter.getSlug(),
                    chapter.getTitleRu(),
                    chapter.getTitleEn(),
                    verse.getOrderIndex(),
                    ChapterService.preview(previewText, 80),
                    verse.getStatus()
            ));
        }

        return new VerseBatchResponseDto(dtos);
    }

    private Map<UUID, Chapter> loadChapters(List<Verse> verses) {
        List<UUID> chapterIds = verses.stream()
                .map(Verse::getChapterId)
                .distinct()
                .toList();
        return chapterRepository.findAllById(chapterIds)
                .stream()
                .collect(Collectors.toMap(Chapter::getId, Function.identity()));
    }

    private Map<UUID, Work> loadWorks(java.util.Collection<Chapter> chapters) {
        List<UUID> workIds = chapters.stream()
                .map(Chapter::getWorkId)
                .distinct()
                .toList();
        return workRepository.findAllById(workIds)
                .stream()
                .collect(Collectors.toMap(Work::getId, Function.identity()));
    }
}