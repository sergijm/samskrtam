package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sm.selflearn.samskrtam.sangraha.dto.ConjugationExamplesResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.ConjugationExamplesSearchRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.DeclensionExamplesSearchRequestDto;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.sangraha.model.Mood;
import sm.selflearn.samskrtam.sangraha.model.Tense;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchResponseDto.VerseDto;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository.VerseCellCount;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository.VerseConjugationCellCount;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Поиск примеров словоформ по словоизменительному классу (sangraha-service.md §9).
 * Поиск не фильтрует по Verse.status — идёт напрямую по наличию подходящей
 * verse_word_morphology (см. §9, B1). Ранжирование — по длине стиха в словах (§9),
 * целиком в SQL (findDeclensionExampleCells), без ранжирования на стороне сервиса:
 * поиск идёт одним запросом к БД (ячейки (caseType, numberType) со стихами), затем
 * один батч-запрос текстов/переводов стихов через VerseBatchService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerseWordSearchService {

    private final VerseWordRepository verseWordRepository;
    private final VerseBatchService verseBatchService;

    /**
     * Один репозиторный вызов на весь запрос: caseType/numberType опциональны (null —
     * фильтр по этому значению не применяется), SQL сам раскрывает кортежи в ячейки
     * парадигмы и обрезает до limitPerGroup на ячейку. Затем тексты стихов добираются
     * одним батч-запросом VerseBatchService; стих, покрывающий несколько ячеек, попадает
     * в каждую, в батч идёт один раз (distinct).
     */
    @Transactional(readOnly = true)
    public DeclensionExamplesResponseDto searchDeclensionExamples(DeclensionExamplesSearchRequestDto request) {
        if (request == null || request.vowelType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "vowelType is required");
        }
        if (request.limitPerGroup() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limitPerGroup must be >= 1");
        }

        String caseType = request.caseType() == null ? null : request.caseType().name();
        String numberType = request.numberType() == null ? null : request.numberType().name();
        List<VerseCellCount> cells = verseWordRepository.findDeclensionExampleCells(
                null, caseType, numberType, request.vowelType().name(),
                request.maxPhraseWords(), request.limitPerGroup());
        if (cells.isEmpty()) {
            return new DeclensionExamplesResponseDto(List.of());
        }

        List<UUID> verseIds = cells.stream()
                .map(VerseCellCount::getVerseId)
                .distinct()
                .toList();
        Map<UUID, VerseDto> verseById = verseBatchService.fetchBatch(
                        new VersesBatchRequestDto(verseIds))
                .verses()
                .stream()
                .collect(Collectors.toMap(VerseDto::verseId, Function.identity()));

        // TreeMap — ячейки в порядке падежей CaseType, затем чисел NumberType (порядок enum).
        Map<CellKey, List<DeclensionExamplesResponseDto.ExampleDto>> byCell = new TreeMap<>(
                Comparator.comparing((CellKey k) -> CaseType.valueOf(k.caseType()))
                        .thenComparing(k -> NumberType.valueOf(k.numberType())));
        for (VerseCellCount cell : cells) {
            VerseDto verse = verseById.get(cell.getVerseId());
            if (verse == null) {
                // стих не найден / удалён — батч его не вернул, ячейку не пополняем
                continue;
            }
            CellKey key = new CellKey(cell.getCaseType(), cell.getNumberType());
            byCell.computeIfAbsent(key, k -> new ArrayList<>()).add(toExampleDto(verse));
        }

        List<DeclensionExamplesResponseDto.GroupDto> groups = byCell.entrySet().stream()
                .map(e -> new DeclensionExamplesResponseDto.GroupDto(
                        e.getKey().caseType(), e.getKey().numberType(), e.getValue()))
                .toList();
        return new DeclensionExamplesResponseDto(groups);
    }

    @Transactional(readOnly = true)
    public ConjugationExamplesResponseDto searchConjugationExamples(ConjugationExamplesSearchRequestDto request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (request.limitPerGroup() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "limitPerGroup must be >= 1");
        }

        String tense = request.tense() == null ? null : request.tense().name();
        String mood = request.mood() == null ? null : request.mood().name();
        List<VerseConjugationCellCount> cells = verseWordRepository.findConjugationExampleCells(
                tense, mood,
                request.maxPhraseWords(), request.limitPerGroup());
        if (cells.isEmpty()) {
            return new ConjugationExamplesResponseDto(List.of());
        }

        List<UUID> verseIds = cells.stream()
                .map(VerseConjugationCellCount::getVerseId)
                .distinct()
                .toList();
        Map<UUID, VerseDto> verseById = verseBatchService.fetchBatch(
                        new VersesBatchRequestDto(verseIds))
                .verses()
                .stream()
                .collect(Collectors.toMap(VerseDto::verseId, Function.identity()));

        // TreeMap — ячейки в порядке перечислений Tense, затем Mood (порядок enum).
        Map<ConjugationCellKey, List<ConjugationExamplesResponseDto.ExampleDto>> byCell = new TreeMap<>(
                Comparator.comparing((ConjugationCellKey k) -> Tense.valueOf(k.tense()))
                        .thenComparing(k -> Mood.valueOf(k.mood())));
        for (VerseConjugationCellCount cell : cells) {
            VerseDto verse = verseById.get(cell.getVerseId());
            if (verse == null) {
                continue;
            }
            ConjugationCellKey key = new ConjugationCellKey(cell.getTense(), cell.getMood());
            byCell.computeIfAbsent(key, k -> new ArrayList<>()).add(toConjugationExampleDto(verse));
        }

        List<ConjugationExamplesResponseDto.GroupDto> groups = byCell.entrySet().stream()
                .map(e -> new ConjugationExamplesResponseDto.GroupDto(
                        e.getKey().tense(), e.getKey().mood(), e.getValue()))
                .toList();
        return new ConjugationExamplesResponseDto(groups);
    }

    private static DeclensionExamplesResponseDto.ExampleDto toExampleDto(VerseDto v) {
        return new DeclensionExamplesResponseDto.ExampleDto(
                v.verseId(), v.workSlug(), v.textIast(), v.textDevanagari(),
                v.translationRu(), v.translationEn(),
                v.workTitleRu(), v.workTitleEn(), v.chapterTitleRu(), v.chapterTitleEn(),
                v.verseOrderIndex());
    }

    private static ConjugationExamplesResponseDto.ExampleDto toConjugationExampleDto(VerseDto v) {
        return new ConjugationExamplesResponseDto.ExampleDto(
                v.verseId(), v.workSlug(), v.textIast(), v.textDevanagari(),
                v.translationRu(), v.translationEn(),
                v.workTitleRu(), v.workTitleEn(), v.chapterTitleRu(), v.chapterTitleEn(),
                v.verseOrderIndex());
    }

    private record CellKey(String caseType, String numberType) {}

    private record ConjugationCellKey(String tense, String mood) {}
}