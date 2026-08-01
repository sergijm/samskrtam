package sm.selflearn.samskrtam.content.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.common.SamskrtamException;
import sm.selflearn.samskrtam.content.dto.DeclensionExamplesResponseDto;
import sm.selflearn.samskrtam.content.dto.DeclensionExamplesResponseDto.ExampleDto;
import sm.selflearn.samskrtam.content.dto.DeclensionExamplesResponseDto.GroupDto;
import sm.selflearn.samskrtam.content.dto.LessonType;
import sm.selflearn.samskrtam.content.dto.SangrahaDeclensionExamplesRequestDto;
import sm.selflearn.samskrtam.content.dto.SangrahaDeclensionExamplesRequestDto.CellDto;
import sm.selflearn.samskrtam.content.dto.SangrahaDeclensionExamplesResponseDto;
import sm.selflearn.samskrtam.content.dto.SangrahaVersesBatchRequestDto;
import sm.selflearn.samskrtam.content.dto.SangrahaVersesBatchResponseDto;
import sm.selflearn.samskrtam.content.dto.SangrahaVersesBatchResponseDto.VerseDto;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.DeclensionExampleGroup;
import sm.selflearn.samskrtam.content.model.DeclensionStem;
import sm.selflearn.samskrtam.content.model.Lesson;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.content.repository.DeclensionExampleGroupRepository;
import sm.selflearn.samskrtam.content.repository.DeclensionStemRepository;
import sm.selflearn.samskrtam.content.repository.LessonRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для вкладки «Примеры» на странице шага склонений
 * (content-service.md §12, declension-examples.md).
 * <p>
 * Координация:
 * 1. Резолв (vowelType, gender) из первого стема урока
 * 2. Для полного набора ячеек (CaseType × NumberType) — чтение кэша declension_example_groups
 * 3. Для незакэшированных ячеек — вызов sangraha declension-examples + upsert кэша
 * 4. Сбор всех verseId → batch-запрос sangraha verses/batch
 * 5. Сбор ответа фронтенду
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeclensionExamplesService {

    /** Сколько примеров запрашивать на одну ячейку (caseType, numberType) */
    private static final int LIMIT_PER_GROUP = 3;

    private final LessonRepository lessonRepository;
    private final DeclensionStemRepository declensionStemRepository;
    private final DeclensionExampleGroupRepository exampleGroupRepository;
    private final SangrahaDeclensionExamplesClient sangrahaClient;
    private final ObjectMapper objectMapper;

    /**
     * Точка входа: GET /content/public/lessons/{slug}/examples.
     *
     * @param slug  slug урока DECLENSIONS
     * @return группы примеров по каждой ячейке парадигмы
     * @throws SamskrtamException если урок не найден, не DECLENSIONS, или не содержит стемов
     */
    @Transactional
    public DeclensionExamplesResponseDto getExamples(String slug) {
        // 1. Резолвим (vowelType, gender) — загружаем стемы урока как в GrammarContentService
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(() -> new SamskrtamException("LESSON_NOT_FOUND",
                        "Lesson not found with slug: " + slug));

        if (lesson.getLessonType() != LessonType.DECLENSIONS) {
            throw new SamskrtamException("LESSON_NOT_FOUND",
                    "Lesson with slug '%s' is not a DECLENSIONS lesson".formatted(slug));
        }

        List<VowelType> vowelTypes = SlugToVowelTypeMapper.mapSlugToVowelTypes(slug);
        List<DeclensionStem> stems;
        if (!vowelTypes.isEmpty()) {
            stems = declensionStemRepository.findByVowelTypeIn(vowelTypes);
        } else {
            stems = declensionStemRepository.findAll();
        }

        // Stable order: by id (UUID)
        stems.sort((a, b) -> a.getId().compareTo(b.getId()));

        if (stems.isEmpty()) {
            throw new SamskrtamException("LESSON_NOT_FOUND",
                    "No stems found for lesson slug '%s'".formatted(slug));
        }

        DeclensionStem firstStem = stems.get(0);
        String vowelTypeStr = firstStem.getVowelType().name();
        String genderStr = firstStem.getGender().name();

        // 2. Полный набор ячеек — все CaseType × NumberType
        List<CellKey> allCells = new ArrayList<>();
        for (CaseType caseType : CaseType.values()) {
            for (NumberType numberType : NumberType.values()) {
                allCells.add(new CellKey(caseType.name(), numberType.name()));
            }
        }

        // 3. Разделяем на кэшированные и незакэшированные ячейки
        List<CachedCell> cachedCells = new ArrayList<>();
        List<CellKey> uncachedCells = new ArrayList<>();

        for (CellKey cell : allCells) {
            exampleGroupRepository.findByVowelTypeAndGenderAndCaseTypeAndNumberType(
                    vowelTypeStr, genderStr, cell.caseType(), cell.numberType())
                    .ifPresentOrElse(
                            group -> cachedCells.add(new CachedCell(cell.caseType(), cell.numberType(),
                                    parseVerseIds(group.getVerseIds()))),
                            () -> uncachedCells.add(cell));
        }

        // 4. Для незакэшированных — зовём sangraha
        if (!uncachedCells.isEmpty()) {
            fetchAndCacheMissing(vowelTypeStr, genderStr, uncachedCells);
            // Перечитываем из кэша и добавляем к cachedCells
            for (CellKey cell : uncachedCells) {
                exampleGroupRepository.findByVowelTypeAndGenderAndCaseTypeAndNumberType(
                        vowelTypeStr, genderStr, cell.caseType(), cell.numberType())
                        .ifPresent(group -> cachedCells.add(new CachedCell(cell.caseType(), cell.numberType(),
                                parseVerseIds(group.getVerseIds()))));
            }
        }

        // 5. Собираем все verseId, убираем дубли
        Set<UUID> allVerseIds = cachedCells.stream()
                .flatMap(c -> c.verseIds().stream())
                .collect(Collectors.toSet());

        // 6. Batch-запрос в sangraha за текстом/переводом стихов
        Map<UUID, VerseDto> verseMap = fetchVerses(allVerseIds);

        // 7. Собираем ответ
        List<GroupDto> groups = new ArrayList<>(cachedCells.size());
        for (CachedCell cell : cachedCells) {
            List<ExampleDto> examples = cell.verseIds().stream()
                    .map(verseMap::get)
                    .filter(v -> v != null) // стих мог быть удалён после кэширования
                    .map(v -> ExampleDto.builder()
                            .verseId(v.getVerseId())
                            .textIast(v.getTextIast())
                            .textDevanagari(v.getTextDevanagari())
                            .translationRu(v.getTranslationRu())
                            .translationEn(v.getTranslationEn())
                            .workTitleRu(v.getWorkTitleRu())
                            .workTitleEn(v.getWorkTitleEn())
                            .chapterTitleRu(v.getChapterTitleRu())
                            .chapterTitleEn(v.getChapterTitleEn())
                            .verseOrderIndex(v.getVerseOrderIndex())
                            .build())
                    .toList();
            if (!examples.isEmpty()) {
                groups.add(new GroupDto(cell.caseType(), cell.numberType(), examples));
            }
        }

        return new DeclensionExamplesResponseDto(groups);
    }

    /**
     * Для незакэшированных ячеек — батч-вызов sangraha declension-examples и upsert кэша.
     */
    private void fetchAndCacheMissing(String vowelType, String gender, List<CellKey> uncachedCells) {
        List<CellDto> cells = uncachedCells.stream()
                .map(c -> new CellDto(c.caseType(), c.numberType()))
                .toList();

        SangrahaDeclensionExamplesRequestDto request = SangrahaDeclensionExamplesRequestDto.builder()
                .vowelType(vowelType)
                .gender(gender)
                .limitPerGroup(LIMIT_PER_GROUP)
                .cells(cells)
                .build();

        SangrahaDeclensionExamplesResponseDto response = sangrahaClient.searchDeclensionExamples(request);

        if (response == null || response.getGroups() == null) {
            log.warn("Sangraha returned null for declension-examples: vowelType={}, gender={}", vowelType, gender);
            // Сохраняем пустые группы для незакэшированных, чтобы не переспрашивать
            for (CellKey cell : uncachedCells) {
                upsertCache(vowelType, gender, cell.caseType(), cell.numberType(), List.of());
            }
            return;
        }

        // Индексируем ответ по (caseType, numberType)
        Map<String, SangrahaDeclensionExamplesResponseDto.GroupDto> groupByKey = new LinkedHashMap<>();
        for (var group : response.getGroups()) {
            groupByKey.put(group.getCaseType() + "|" + group.getNumberType(), group);
        }

        for (CellKey cell : uncachedCells) {
            String key = cell.caseType() + "|" + cell.numberType();
            List<UUID> verseIds = groupByKey.containsKey(key)
                    ? groupByKey.get(key).getVerseIds()
                    : List.of();
            upsertCache(vowelType, gender, cell.caseType(), cell.numberType(), verseIds);
        }
    }

    /**
     * Сохранить/обновить строку в declension_example_groups (в том числе пустой массив).
     */
    private void upsertCache(String vowelType, String gender, String caseType, String numberType, List<UUID> verseIds) {
        String verseIdsJson;
        try {
            verseIdsJson = objectMapper.writeValueAsString(verseIds);
        } catch (Exception e) {
            log.warn("Failed to serialize verseIds for cache: {}", e.getMessage());
            verseIdsJson = "[]";
        }

        DeclensionExampleGroup group = DeclensionExampleGroup.builder()
                .vowelType(vowelType)
                .gender(gender)
                .caseType(caseType)
                .numberType(numberType)
                .verseIds(verseIdsJson)
                .createdAt(Instant.now())
                .build();

        try {
            exampleGroupRepository.save(group);
        } catch (Exception e) {
            // UNIQUE constraint violation — другой запрос уже вставил строку.
            log.debug("Cache upsert conflict (concurrent): {}/{}/{}/{}", vowelType, gender, caseType, numberType);
        }
    }

    /**
     * Батч-запрос стихов по verseId. Возвращает Map<verseId, VerseDto>.
     */
    private Map<UUID, VerseDto> fetchVerses(Set<UUID> verseIds) {
        if (verseIds.isEmpty()) {
            return Collections.emptyMap();
        }
        SangrahaVersesBatchRequestDto request = SangrahaVersesBatchRequestDto.builder()
                .verseIds(List.copyOf(verseIds))
                .build();
        SangrahaVersesBatchResponseDto response = sangrahaClient.fetchVersesBatch(request);
        if (response == null || response.getVerses() == null) {
            return Collections.emptyMap();
        }
        return response.getVerses().stream()
                .collect(Collectors.toMap(VerseDto::getVerseId, v -> v));
    }

    /**
     * Парсит JSON-массив UUID из verse_ids JSONB.
     */
    private List<UUID> parseVerseIds(String json) {
        if (json == null || json.isBlank() || "[]".equals(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse verse_ids JSON: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Внутреннее DTO для связки (caseType, numberType) — ключ ячейки.
     */
    private record CellKey(String caseType, String numberType) {}

    /**
     * Внутреннее DTO для связки (caseType, numberType, verseIds) из кэша.
     */
    private record CachedCell(String caseType, String numberType, List<UUID> verseIds) {}
}