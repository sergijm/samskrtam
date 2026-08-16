package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeFrequency;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeFrequencyId;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticClassRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Batch-импорт лексики из sangraha-service (endpoint lemmas/export, см. §7
 * lexicon-content-pipeline.md). Данные приходят уже агрегированными: одна строка
 * — одна пара (лемма, род) с частотой, грамматикой и семантической классификацией.
 * curriculum-service делает простой upsert: лексема + POS + морфология + частота +
 * семантические темы (из categoryCodes классификации).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LexiconImportService {

    public static final String FREQUENCY_SOURCE = "SANGRAHA_CORPUS";
    static final int PAGE_LIMIT = 500;

    private final SangrahaExportClient sangrahaExportClient;
    private final LexemeRepository lexemeRepository;
    private final LexemeFrequencyRepository lexemeFrequencyRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final MorphologyClassRepository morphologyClassRepository;
    private final SemanticClassRepository semanticClassRepository;

    @Transactional
    public SangrahaImportResult importFromSangraha() {
        List<LemmaExportItem> rows = fetchAll();

        int imported = 0;
        int updated = 0;

        for (int i = 0; i < rows.size(); i++) {
            LemmaExportItem row = rows.get(i);
            LexemeGender gender = parseGender(row.gender());

            Lexeme lexeme = lexemeRepository.findByLemmaSlp1AndGender(row.lemmaSlp1(), gender)
                    .orElseGet(() -> {
                        Lexeme created = new Lexeme();
                        created.setLemmaSlp1(row.lemmaSlp1());
                        created.setLemmaIast(row.lemmaIast());
                        created.setLemmaDevanagari(row.lemmaDevanagari());
                        created.setGlossRu(emptyIfNull(row.glossRu()));
                        created.setGlossEn(emptyIfNull(row.glossEn()));
                        created.setGender(gender);
                        created.setMeaningNumber(lexemeRepository.findMaxMeaningNumber(row.lemmaSlp1()) + 1);
                        return created;
                    });
            boolean isNew = lexeme.getId() == null;

            if (!isBlank(row.glossRu()) && isBlank(lexeme.getGlossRu())) {
                lexeme.setGlossRu(row.glossRu());
            }
            if (!isBlank(row.glossEn()) && isBlank(lexeme.getGlossEn())) {
                lexeme.setGlossEn(row.glossEn());
            }

            if (isNew) {
                lexeme = lexemeRepository.save(lexeme);
                imported++;
            } else {
                updated++;
            }

            attachFrequency(lexeme, i + 1);
            attachPartOfSpeech(lexeme, mapPos(row.dominantPosCode()));
            attachMorphology(lexeme, mapMorphologyCode(row.gender(), row.vowelType()));
            attachSemanticClasses(lexeme, row.categoryCodes());
        }

        int total = (int) lexemeRepository.count();
        SangrahaImportResult result = new SangrahaImportResult(imported, updated, total);
        log.info("Sangraha import done: imported={}, updated={}, total={}",
                imported, updated, total);
        return result;
    }

    private List<LemmaExportItem> fetchAll() {
        List<LemmaExportItem> rows = new ArrayList<>();
        UUID cursor = null;
        int guard = 0;
        while (true) {
            LemmaExportPage page = sangrahaExportClient.fetchLemmaExport(cursor, PAGE_LIMIT);
            if (page == null || page.items() == null || page.items().isEmpty()) {
                break;
            }
            rows.addAll(page.items());
            if (page.nextCursor() == null || ++guard > 10_000) {
                break;
            }
            cursor = page.nextCursor();
        }
        log.info("Lemma export: {} rows fetched", rows.size());
        return rows;
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

    private void attachMorphology(Lexeme lexeme, String code) {
        if (code == null) {
            return;
        }
        if (lexeme.getMorphologyClasses().stream().anyMatch(m -> m.getCode().equals(code))) {
            return;
        }
        MorphologyClass mc = morphologyClassRepository.findByCode(code).orElse(null);
        if (mc == null) {
            return;
        }
        lexeme.getMorphologyClasses().add(mc);
    }

    private void attachSemanticClasses(Lexeme lexeme, List<String> categoryCodes) {
        if (categoryCodes == null || categoryCodes.isEmpty()) {
            return;
        }
        for (String code : categoryCodes) {
            if (isBlank(code)) {
                continue;
            }
            SemanticClass topic = semanticClassRepository.findByCode(code).orElse(null);
            if (topic == null) {
                continue;
            }
            if (lexeme.getSemanticClasses().stream().anyMatch(t -> t.getCode().equals(code))) {
                continue;
            }
            lexeme.getSemanticClasses().add(topic);
        }
    }

    /**
     * sangraha {@code PartOfSpeech} → curriculum {@code part_of_speech.code}.
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
     * {@code VowelType} → curriculum {@code morphology_class.code}.
     */
    static String mapMorphologyCode(String gender, String vowelType) {
        if (vowelType == null) {
            return null;
        }
        return switch (vowelType) {
            case "A_STEM" -> {
                LexemeGender g = parseGender(gender);
                yield g == LexemeGender.NEUTER ? "a-stem-neut"
                        : g == LexemeGender.FEMININE ? "a-stem-fem" : "a-stem-masc";
            }
            case "AA_STEM" -> "a-stem-fem";
            case "I_STEM" -> "i-stem";
            case "II_STEM" -> "ii-stem";
            case "U_STEM" -> "u-stem";
            case "UU_STEM" -> "uu-stem";
            case "R_STEM" -> "r-stem";
            default -> null;
        };
    }

    static LexemeGender parseGender(String gender) {
        if (gender == null) {
            return null;
        }
        try {
            return LexemeGender.valueOf(gender);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}