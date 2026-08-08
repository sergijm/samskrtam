package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.*;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.*;
import sm.selflearn.samskrtam.curriculum.lexicon.service.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Детерминированный (без LLM) batch-импорт лексики из корпуса sangraha-service
 * (lexicon-content-pipeline.md §2). Группирует выгруженные VerseWord по
 * (lemmaSlp1, gender), считает frequencyRank по COUNT(*), маппит POS/gender/
 * morphology-класс эвристикой и идемпотентно делает upsert в curriculum.lexeme.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LexiconImportService {

    /** Константа-источник частотного списка (lexicon.md §2: «например CURATED_2000»). */
    public static final String FREQUENCY_SOURCE = "SANGRAHA_CORPUS";
    public static final int PAGE_LIMIT = 500;

    private final SangrahaExportClient sangrahaExportClient;
    private final TransliterationService transliterationService;

    private final LexemeRepository lexemeRepository;
    private final LexemeFrequencyRepository lexemeFrequencyRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final MorphologyClassRepository morphologyClassRepository;
    private final SourceRepository sourceRepository;
    private final SourceOccurrenceRepository sourceOccurrenceRepository;

    /**
     * Полный проход: страницы до nextCursor = null, группировка, частоты,
     * upsert. Идемпотентен — повторный запуск на тех же данных не создаёт
     * дублей Lexeme/SourceOccurrence, только пересчитывает частоты/кэши.
     */
    @Transactional
    public SangrahaImportResult importFromSangraha() {
        List<VerseWordExportItem> rows = fetchAll();

        Map<GroupKey, List<VerseWordExportItem>> groups = groupByLemmaAndGender(rows);
        List<GroupedLexeme> ranked = rankGroups(groups);

        int imported = 0;
        int updated = 0;
        Set<UUID> touchedSources = new HashSet<>();

        for (GroupedLexeme grouped : ranked) {
            Lexeme lexeme = lexemeRepository.findByLemmaSlp1AndGender(
                            grouped.slp1(), grouped.gender())
                    .orElseGet(() -> {
                        Lexeme created = new Lexeme();
                        created.setLemmaSlp1(grouped.slp1());
                        created.setLemmaIast(grouped.representative().lemmaIast());
                        created.setLemmaDevanagari(
                                transliterationService.iastToDevanagari(grouped.representative().lemmaIast()));
                        created.setGlossRu(emptyIfNull(grouped.representative().lemmaGlossRu()));
                        created.setGlossEn(emptyIfNull(grouped.representative().lemmaGlossEn()));
                        created.setGender(grouped.gender());
                        created.setStatus(LexemeStatus.CANDIDATE);
                        return created;
                    });
            boolean isNew = lexeme.getId() == null;
            if (isNew) {
                lexeme = lexemeRepository.save(lexeme);
                imported++;
            } else {
                updated++;
            }

            attachFrequency(lexeme, grouped.rank());
            attachPartOfSpeech(lexeme, grouped.posCode());
            attachMorphology(lexeme, grouped.morphologyCode());

            for (VerseWordExportItem row : grouped.items()) {
                Source source = sourceRepository.findByExternalSangrahaWorkSlug(row.workSlug())
                        .orElseGet(() -> {
                            Source created = new Source();
                            created.setCode("sangraha:" + row.workSlug());
                            created.setTitleRu(row.workSlug());
                            created.setTitleEn(row.workSlug());
                            created.setKind(SourceKind.OTHER);
                            created.setExternalSangrahaWorkSlug(row.workSlug());
                            created.setTotalOccurrencesCache(0);
                            created.setUniqueLemmaCountCache(0);
                            return sourceRepository.save(created);
                        });
                touchedSources.add(source.getId());
                upsertOccurrence(source, lexeme, row);
            }
        }

        cleanupGlosslessCandidates();

        recalcSourceCaches(touchedSources);

        int total = (int) lexemeRepository.count();
        SangrahaImportResult result = new SangrahaImportResult(
                imported, updated, total, touchedSources.size());
        log.info("Sangraha import done: imported={}, updated={}, total={}, sources={}",
                imported, updated, total, touchedSources.size());
        return result;
    }

    private List<VerseWordExportItem> fetchAll() {
        List<VerseWordExportItem> rows = new ArrayList<>();
        UUID cursor = null;
        int guard = 0;
        while (true) {
            VerseWordExportPage page = sangrahaExportClient.fetchPage(cursor, PAGE_LIMIT);
            if (page == null || page.items() == null || page.items().isEmpty()) {
                break;
            }
            rows.addAll(page.items());
            if (page.nextCursor() == null || ++guard > 10_000) {
                break;
            }
            cursor = page.nextCursor();
        }
        log.info("Sangraha export: {} rows fetched", rows.size());
        return rows;
    }

    private Map<GroupKey, List<VerseWordExportItem>> groupByLemmaAndGender(List<VerseWordExportItem> rows) {
        Map<GroupKey, List<VerseWordExportItem>> groups = new LinkedHashMap<>();
        for (VerseWordExportItem row : rows) {
            if (!hasGloss(row)) {
                // Без перевода нельзя построить вопрос квиза — такие строки не импортируем.
                continue;
            }
            String slp1 = transliterationService.iastToSlp1(row.lemmaIast());
            if (slp1 == null || slp1.isBlank()) {
                continue;
            }
            GroupKey key = new GroupKey(slp1, parseGender(row.gender()));
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }
        return groups;
    }

    /** Есть хотя бы RU- или EN-глосс — иначе лексему нельзя использовать в квизе. */
    private static boolean hasGloss(VerseWordExportItem row) {
        return !isBlank(row.lemmaGlossRu()) || !isBlank(row.lemmaGlossEn());
    }

    /**
     * Сортировка групп по убыванию occurrenceCount, tie-break — алфавит по
     * lemmaSlp1 (pipeline §2 шаг 2). rank = позиция в отсортированном списке.
     */
    private List<GroupedLexeme> rankGroups(Map<GroupKey, List<VerseWordExportItem>> groups) {
        List<GroupedLexeme> ranked = groups.entrySet().stream()
                .map(entry -> {
                    List<VerseWordExportItem> items = entry.getValue();
                    VerseWordExportItem representative = pickRepresentative(items);
                    String posCode = mapPos(representative.pos());
                    String morphologyCode = mapMorphology(
                            representative.vowelType(), representative.stem(), entry.getKey().gender());
                    return new GroupedLexeme(
                            entry.getKey().slp1(),
                            entry.getKey().gender(),
                            items.size(),
                            items,
                            representative,
                            posCode,
                            morphologyCode,
                            0);
                })
                .sorted(Comparator
                        .comparingInt(GroupedLexeme::occurrenceCount).reversed()
                        .thenComparing(GroupedLexeme::slp1))
                .toList();

        List<GroupedLexeme> withRanks = new ArrayList<>(ranked.size());
        for (int i = 0; i < ranked.size(); i++) {
            GroupedLexeme g = ranked.get(i);
            withRanks.add(new GroupedLexeme(
                    g.slp1(), g.gender(), g.occurrenceCount(), g.items(),
                    g.representative(), g.posCode(), g.morphologyCode(), i + 1));
        }
        return withRanks;
    }

    /**
     * Представитель группы: строка с наибольшим числом вхождений (lemmaIast, stem),
     * tie-break — наименьший verseId (pipeline §2 шаг 3, task §13).
     * Отдаём предпочтение строкам, у которых есть перевод (без него нельзя строить вопрос).
     */
    static VerseWordExportItem pickRepresentative(List<VerseWordExportItem> items) {
        Map<String, Integer> counts = new HashMap<>();
        Map<String, VerseWordExportItem> representativeBySig = new HashMap<>();
        for (VerseWordExportItem item : items) {
            String sig = item.lemmaIast() + "\u0000" + item.stem();
            counts.merge(sig, 1, Integer::sum);
            VerseWordExportItem current = representativeBySig.get(sig);
            if (current == null || isBefore(item, current)) {
                representativeBySig.put(sig, item);
            }
        }
        return counts.entrySet().stream()
                .max(Comparator
                        .comparingInt(Map.Entry<String, Integer>::getValue)
                        .thenComparingInt(entry -> hasGloss(representativeBySig.get(entry.getKey())) ? 1 : 0)
                        .thenComparing(entry -> representativeBySig.get(entry.getKey()).verseId(),
                                Comparator.reverseOrder()))
                .map(entry -> representativeBySig.get(entry.getKey()))
                .orElse(items.get(0));
    }

    /** Дешёвое сравнение «по verseId», безопасное при null. */
    private static boolean isBefore(VerseWordExportItem candidate, VerseWordExportItem current) {
        if (candidate.verseId() == null) {
            return false;
        }
        if (current.verseId() == null) {
            return true;
        }
        return candidate.verseId().compareTo(current.verseId()) < 0;
    }

    /**
     * sangraha pos → curriculum.part_of_speech.code (task §10): постоянная карта,
     * при неизвестном коде — null, без исключения.
     */
    static String mapPos(String sangrahaPos) {
        if (sangrahaPos == null) {
            return null;
        }
        return switch (sangrahaPos) {
            case "NOUN" -> "noun";
            case "VERB" -> "finite-verb";
            case "ADJECTIVE" -> "adjective";
            case "PRONOUN" -> "pronoun";
            case "ADVERB" -> "adverb";
            case "PARTICLE" -> "particle";
            case "INDECLINABLE" -> null;
            case "NUMERAL" -> "numeral";
            case "CONJUNCTION" -> "conjunction";
            case "INTERJECTION" -> "interjection";
            case "OTHER" -> null;
            default -> null;
        };
    }

    /**
     * vowelType/verb-класс → morphology_class.code (task §12): маппинг 1:1,
     * fallback по последней букве stem, если vowelType не заполнен
     * (sangraha-service.md §9 — тот же принцип classifyVowelType).
     */
    static String mapMorphology(String vowelType, String stem, LexemeGender gender) {
        if (vowelType != null) {
            String mapped = switch (vowelType) {
                case "A_STEM" -> aStem(gender);
                case "AA_STEM" -> "a-stem-fem";
                case "I_STEM" -> "i-stem";
                case "U_STEM" -> "u-stem";
                case "R_STEM" -> "r-stem";
                default -> null;
            };
            if (mapped != null) {
                return mapped;
            }
        }
        return classifyStem(stem, gender);
    }

    /**
     * Fallback по последней букве stem (как sangraha-service.md §9 REGULAR_LAST_LETTERS):
     * a→a-stem(-masc/-neut по роду), ā→a-stem-fem, i→i-stem, ī→i-stem, u→u-stem,
     * ū→u-stem, ṛ/r→r-stem.
     */
    static String classifyStem(String stem, LexemeGender gender) {
        if (stem == null || stem.isEmpty()) {
            return null;
        }
        char last = stem.charAt(stem.length() - 1);
        return switch (last) {
            case 'a' -> aStem(gender);
            case 'ā' -> "a-stem-fem";
            case 'i', 'ī' -> "i-stem";
            case 'u', 'ū' -> "u-stem";
            case 'ṛ', 'r' -> "r-stem";
            default -> null;
        };
    }

    private static String aStem(LexemeGender gender) {
        if (gender == LexemeGender.NEUTER) {
            return "a-stem-neut";
        }
        if (gender == LexemeGender.FEMININE) {
            return "a-stem-fem";
        }
        return "a-stem-masc";
    }

    private void attachFrequency(Lexeme lexeme, int rank) {
        LexemeFrequencyId id = new LexemeFrequencyId();
        id.setLexemeId(lexeme.getId());
        id.setSource(FREQUENCY_SOURCE);
        LexemeFrequency frequency = lexemeFrequencyRepository.findById(id).orElseGet(() -> {
            LexemeFrequency created = new LexemeFrequency();
            created.setId(id);
            created.setLexeme(lexeme);
            return created;
        });
        frequency.setRank(rank);
        lexemeFrequencyRepository.save(frequency);
    }

    private void attachPartOfSpeech(Lexeme lexeme, String posCode) {
        if (posCode == null) {
            return;
        }
        if (lexeme.getPartsOfSpeech().stream().anyMatch(p -> p.getCode().equals(posCode))) {
            return;
        }
        PartOfSpeech pos = partOfSpeechRepository.findByCode(posCode).orElse(null);
        if (pos == null) {
            return;
        }
        lexeme.getPartsOfSpeech().add(pos);
    }

    private void attachMorphology(Lexeme lexeme, String morphologyCode) {
        if (morphologyCode == null) {
            return;
        }
        if (lexeme.getMorphologyClasses().stream().anyMatch(m -> m.getCode().equals(morphologyCode))) {
            return;
        }
        MorphologyClass morphologyClass = morphologyClassRepository.findByCode(morphologyCode).orElse(null);
        if (morphologyClass == null) {
            return;
        }
        lexeme.getMorphologyClasses().add(morphologyClass);
    }

    private void upsertOccurrence(Source source, Lexeme lexeme, VerseWordExportItem row) {
        String locationRef = row.chapterSlug() + "." + row.verseOrderIndex();
        List<SourceOccurrence> existing = sourceOccurrenceRepository
                .findBySourceIdAndLocationRef(source.getId(), locationRef);
        boolean exists = existing.stream()
                .anyMatch(o -> o.getLexeme().getId().equals(lexeme.getId())
                        && o.getSurfaceFormIast().equals(row.surfaceIast()));
        if (exists) {
            return;
        }
        SourceOccurrence occurrence = new SourceOccurrence();
        occurrence.setSource(source);
        occurrence.setLexeme(lexeme);
        occurrence.setLocationRef(locationRef);
        occurrence.setSurfaceFormIast(row.surfaceIast());
        sourceOccurrenceRepository.save(occurrence);
    }

    private void recalcSourceCaches(Set<UUID> sourceIds) {
        for (UUID sourceId : sourceIds) {
            Source source = sourceRepository.findById(sourceId).orElse(null);
            if (source == null) {
                continue;
            }
            long total = sourceOccurrenceRepository.countBySourceId(sourceId);
            long unique = sourceOccurrenceRepository.countDistinctBySourceId(sourceId);
            source.setTotalOccurrencesCache((int) total);
            source.setUniqueLemmaCountCache((int) unique);
            sourceRepository.save(source);
        }
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static LexemeGender parseGender(String gender) {
        if (gender == null) {
            return null;
        }
        try {
            return LexemeGender.valueOf(gender);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }


    /**
     * Удаляет лексемы со статусом CANDIDATE, у которых нет ни RU-, ни EN-глосса.
     * Они не могли появиться при нормальном импорте (groupByLemmaAndGender отбрасывает
     * строки без глосса), но могут остаться после ручных правок или предыдущих версий.
     */
    private void cleanupGlosslessCandidates() {
        List<Lexeme> candidates = lexemeRepository.findByStatus(LexemeStatus.CANDIDATE);
        List<Lexeme> toDelete = candidates.stream()
                .filter(l -> isBlank(l.getGlossRu()) && isBlank(l.getGlossEn()))
                .toList();
        if (!toDelete.isEmpty()) {
            log.info("Cleaning up {} glossless CANDIDATE lexemes", toDelete.size());
            lexemeRepository.deleteAll(toDelete);
        }
    }

    private record GroupKey(String slp1, LexemeGender gender) {}

    private record GroupedLexeme(
            String slp1,
            LexemeGender gender,
            int occurrenceCount,
            List<VerseWordExportItem> items,
            VerseWordExportItem representative,
            String posCode,
            String morphologyCode,
            int rank
    ) {}
}
