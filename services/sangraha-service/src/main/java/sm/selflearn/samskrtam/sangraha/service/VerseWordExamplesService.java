package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.LemmaExamplesRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.LemmaExamplesResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordExamplesRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.VerseWordExamplesResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchRequestDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchResponseDto;
import sm.selflearn.samskrtam.sangraha.dto.VersesBatchResponseDto.VerseDto;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository.LemmaVerseRank;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository.SurfaceVerseRank;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Поиск примера стиха по точной словоформе (surfaceIast) для колонки «примеры из
 * санграхи» в таблице слов урока склонений.
 * <p>
 * Для каждой запрошенной формы возвращается ровно один стих — самый короткий
 * (3–7 слов), содержащий глагол. Форма без подходящего стиха получает пустой
 * verses (ячейка остаётся пустой). Стих без перевода отдаётся как есть.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VerseWordExamplesService {

    private static final int MIN_WORDS = 3;
    private static final int MAX_WORDS = 7;

    private final VerseWordRepository verseWordRepository;
    private final VerseBatchService verseBatchService;

    @Transactional(readOnly = true)
    public VerseWordExamplesResponseDto findExamples(VerseWordExamplesRequestDto request) {
        List<String> surfaceIasts = request.surfaceIasts().stream()
                .filter(form -> form != null && !form.isBlank())
                .distinct()
                .toList();
        if (surfaceIasts.isEmpty()) {
            return new VerseWordExamplesResponseDto(List.of());
        }

        // Для каждой формы — самый короткий стих с глаголом (3–7 слов).
        List<SurfaceVerseRank> ranks = verseWordRepository
                .findShortestSurfaceVerseWithVerb(surfaceIasts, MIN_WORDS, MAX_WORDS);
        Map<String, UUID> verseIdByForm = ranks.stream()
                .collect(Collectors.toMap(
                        SurfaceVerseRank::getSurfaceIast,
                        SurfaceVerseRank::getVerseId,
                        (a, b) -> a,
                        LinkedHashMap::new));

        // Загружаем детали выбранных стихов одним батчем и раскладываем по verseId.
        List<UUID> allVerseIds = verseIdByForm.values().stream().distinct().toList();
        Map<UUID, VerseDto> verseById = fetchVerseDtos(allVerseIds);

        List<VerseWordExamplesResponseDto.ResultDto> results = surfaceIasts.stream()
                .map(form -> {
                    UUID verseId = verseIdByForm.get(form);
                    VerseDto verse = verseId == null ? null : verseById.get(verseId);
                    return new VerseWordExamplesResponseDto.ResultDto(
                            form,
                            verse == null ? List.of() : List.of(verse));
                })
                .toList();
        return new VerseWordExamplesResponseDto(results);
    }

    private Map<UUID, VerseDto> fetchVerseDtos(List<UUID> verseIds) {
        if (verseIds.isEmpty()) {
            return Map.of();
        }
        VersesBatchResponseDto batch = verseBatchService.fetchBatch(new VersesBatchRequestDto(verseIds));
        return batch.verses().stream()
                .collect(Collectors.toMap(VerseDto::verseId, Function.identity()));
    }

    /**
     * Поиск примеров стихов по леммам (словарным формам) для раскрываемых строк
     * таблицы слов лексического урока. Для каждой запрошенной леммы возвращается
     * до {@code limitPerLemma} стихов, содержащих эту лемму. Лемма без подходящих
     * стихов получает пустой verses; порядок results повторяет порядок lemmas.
     */
    @Transactional(readOnly = true)
    public LemmaExamplesResponseDto findLemmaExamples(LemmaExamplesRequestDto request) {
        List<String> lemmas = request.lemmas().stream()
                .filter(l -> l != null && !l.isBlank())
                .distinct()
                .toList();
        int limit = Math.max(1, request.limitPerLemma());
        if (lemmas.isEmpty()) {
            return new LemmaExamplesResponseDto(List.of());
        }

        List<LemmaVerseRank> ranks = verseWordRepository
                .findLemmaVerseIds(lemmas, limit);
        // lemma → упорядоченный список verseId (до limit на лемму), без дублей.
        Map<String, List<UUID>> verseIdsByLemma = new LinkedHashMap<>();
        for (LemmaVerseRank rank : ranks) {
            verseIdsByLemma
                    .computeIfAbsent(rank.getLemmaIast(), k -> new ArrayList<>())
                    .add(rank.getVerseId());
        }

        List<UUID> allVerseIds = verseIdsByLemma.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
        Map<UUID, VerseDto> verseById = fetchVerseDtos(allVerseIds);

        List<LemmaExamplesResponseDto.ResultDto> results = lemmas.stream()
                .map(lemma -> {
                    List<UUID> verseIds = verseIdsByLemma.getOrDefault(lemma, List.of());
                    List<VerseDto> verses = verseIds.stream()
                            .map(verseById::get)
                            .filter(v -> v != null)
                            .toList();
                    return new LemmaExamplesResponseDto.ResultDto(lemma, verses);
                })
                .toList();
        return new LemmaExamplesResponseDto(results);
    }
}
