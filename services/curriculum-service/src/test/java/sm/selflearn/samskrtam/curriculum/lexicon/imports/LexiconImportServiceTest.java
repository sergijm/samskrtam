package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeFrequency;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyAppliesTo;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PosGroup;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticClassRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LexiconImportServiceTest {

    private static LemmaExportItem item(String slp1, String iast, String devanagari,
                                        String gender, String pos, int count,
                                        List<String> categories, String glossRu, String glossEn,
                                        String vowelType) {
        return new LemmaExportItem(UUID.randomUUID(), slp1, iast, devanagari, gender, pos,
                count, categories, glossRu, glossEn, vowelType);
    }

    private LexiconImportService service(
            SangrahaExportClient client,
            LexemeRepository lexemeRepo,
            LexemeFrequencyRepository freqRepo,
            PartOfSpeechRepository posRepo,
            MorphologyClassRepository morphologyRepo,
            SemanticClassRepository semanticClassRepo) {
        return new LexiconImportService(client, lexemeRepo, freqRepo, posRepo, morphologyRepo, semanticClassRepo);
    }

    @Test
    void importFromSangraha_bindsSemanticClassesAndMapsGrammar() {
        LemmaExportItem nara = item("nara", "nara", "नर", "MASCULINE", "NOUN", 10,
                List.of("people-family"), "мужчина", "man", "A_STEM");
        SangrahaExportClient client = mock(SangrahaExportClient.class);
        when(client.fetchLemmaExport(any(), anyInt()))
                .thenReturn(new LemmaExportPage(List.of(nara), null));

        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        List<Lexeme> saved = new ArrayList<>();
        when(lexemeRepo.findByLemmaSlp1AndGender(any(), any())).thenReturn(Optional.empty());
        when(lexemeRepo.save(any())).thenAnswer(inv -> {
            Lexeme l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(UUID.randomUUID());
            }
            saved.add(l);
            return l;
        });
        when(lexemeRepo.count()).thenReturn(1L);

        PartOfSpeech noun = new PartOfSpeech();
        noun.setCode("noun");
        noun.setGroup(PosGroup.NOMINAL);
        PartOfSpeechRepository posRepo = mock(PartOfSpeechRepository.class);
        when(posRepo.findByCode("noun")).thenReturn(Optional.of(noun));

        MorphologyClass aStem = new MorphologyClass();
        aStem.setCode("a-stem-masc");
        aStem.setAppliesTo(MorphologyAppliesTo.NOUN);
        MorphologyClassRepository morphologyRepo = mock(MorphologyClassRepository.class);
        when(morphologyRepo.findByCode("a-stem-masc")).thenReturn(Optional.of(aStem));

        SemanticClass family = new SemanticClass();
        family.setCode("people-family");
        SemanticClassRepository semanticClassRepo = mock(SemanticClassRepository.class);
        when(semanticClassRepo.findByCode("people-family")).thenReturn(Optional.of(family));

        LexemeFrequencyRepository freqRepo = mock(LexemeFrequencyRepository.class);
        List<LexemeFrequency> freqs = new ArrayList<>();
        when(freqRepo.findById(any())).thenReturn(Optional.empty());
        when(freqRepo.save(any())).thenAnswer(inv -> {
            LexemeFrequency f = inv.getArgument(0);
            freqs.add(f);
            return f;
        });

        LexiconImportService importService =
                service(client, lexemeRepo, freqRepo, posRepo, morphologyRepo, semanticClassRepo);

        SangrahaImportResult result = importService.importFromSangraha();

        assertThat(result.importedCount()).isEqualTo(1);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.totalLexemeCount()).isEqualTo(1);

        Lexeme savedLexeme = saved.get(0);
        assertThat(savedLexeme.getSemanticClasses()).extracting(SemanticClass::getCode)
                .containsExactly("people-family");
        assertThat(savedLexeme.getPartsOfSpeech()).extracting(PartOfSpeech::getCode)
                .containsExactly("noun");
        assertThat(savedLexeme.getMorphologyClasses()).extracting(MorphologyClass::getCode)
                .containsExactly("a-stem-masc");
        assertThat(freqs).extracting(LexemeFrequency::getRank).containsExactly(1);
    }

    @Test
    void mapPos_knownAndUnknownCodes() {
        assertThat(LexiconImportService.mapPos("NOUN")).isEqualTo("noun");
        assertThat(LexiconImportService.mapPos("VERB")).isEqualTo("finite-verb");
        assertThat(LexiconImportService.mapPos("ADJECTIVE")).isEqualTo("adjective");
        assertThat(LexiconImportService.mapPos("INDECLINABLE")).isNull();
        assertThat(LexiconImportService.mapPos("UNKNOWN")).isNull();
        assertThat(LexiconImportService.mapPos(null)).isNull();
    }

    @Test
    void mapMorphologyCode_vowelTypeToMorphologyClass() {
        assertThat(LexiconImportService.mapMorphologyCode("MASCULINE", "A_STEM"))
                .isEqualTo("a-stem-masc");
        assertThat(LexiconImportService.mapMorphologyCode("NEUTER", "A_STEM"))
                .isEqualTo("a-stem-neut");
        assertThat(LexiconImportService.mapMorphologyCode("FEMININE", "A_STEM"))
                .isEqualTo("a-stem-fem");
        assertThat(LexiconImportService.mapMorphologyCode("FEMININE", "AA_STEM"))
                .isEqualTo("a-stem-fem");
        assertThat(LexiconImportService.mapMorphologyCode("MASCULINE", "I_STEM"))
                .isEqualTo("i-stem");
        assertThat(LexiconImportService.mapMorphologyCode("MASCULINE", "II_STEM"))
                .isEqualTo("ii-stem");
        assertThat(LexiconImportService.mapMorphologyCode("MASCULINE", "U_STEM"))
                .isEqualTo("u-stem");
        assertThat(LexiconImportService.mapMorphologyCode("MASCULINE", "UU_STEM"))
                .isEqualTo("uu-stem");
        assertThat(LexiconImportService.mapMorphologyCode("MASCULINE", "R_STEM"))
                .isEqualTo("r-stem");
        assertThat(LexiconImportService.mapMorphologyCode("MASCULINE", null)).isNull();
        assertThat(LexiconImportService.mapMorphologyCode("MASCULINE", "PRON_AHAM")).isNull();
    }

    @Test
    void importFromSangraha_secondRunOnSameData_createsNoDuplicates() {
        LemmaExportItem nara = item("nara", "nara", "नर", "MASCULINE", "NOUN", 10,
                List.of("people-family"), "мужчина", "man", "A_STEM");
        SangrahaExportClient client = mock(SangrahaExportClient.class);
        when(client.fetchLemmaExport(any(), anyInt()))
                .thenReturn(new LemmaExportPage(List.of(nara), null));

        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        java.util.Map<String, Lexeme> lexemesByKey = new java.util.HashMap<>();
        when(lexemeRepo.findByLemmaSlp1AndGender(any(), any())).thenAnswer(inv -> {
            String slp1 = inv.getArgument(0);
            LexemeGender gender = inv.getArgument(1);
            return Optional.ofNullable(lexemesByKey.get(slp1 + "|" + gender));
        });
        when(lexemeRepo.save(any())).thenAnswer(inv -> {
            Lexeme l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(UUID.randomUUID());
            }
            lexemesByKey.put(l.getLemmaSlp1() + "|" + l.getGender(), l);
            return l;
        });
        when(lexemeRepo.count()).thenAnswer(inv -> (long) lexemesByKey.size());

        LexemeFrequencyRepository freqRepo = mock(LexemeFrequencyRepository.class);
        when(freqRepo.findById(any())).thenReturn(Optional.empty());
        when(freqRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PartOfSpeechRepository posRepo = mock(PartOfSpeechRepository.class);
        PartOfSpeech noun = new PartOfSpeech();
        noun.setCode("noun");
        noun.setGroup(PosGroup.NOMINAL);
        when(posRepo.findByCode("noun")).thenReturn(Optional.of(noun));

        MorphologyClassRepository morphologyRepo = mock(MorphologyClassRepository.class);
        when(morphologyRepo.findByCode(any())).thenAnswer(inv -> {
            MorphologyClass mc = new MorphologyClass();
            mc.setCode(inv.getArgument(0));
            return Optional.of(mc);
        });

        SemanticClassRepository semanticClassRepo = mock(SemanticClassRepository.class);
        SemanticClass family = new SemanticClass();
        family.setCode("people-family");
        when(semanticClassRepo.findByCode("people-family")).thenReturn(Optional.of(family));

        LexiconImportService importService =
                service(client, lexemeRepo, freqRepo, posRepo, morphologyRepo, semanticClassRepo);

        SangrahaImportResult first = importService.importFromSangraha();
        SangrahaImportResult second = importService.importFromSangraha();

        assertThat(first.importedCount()).isEqualTo(1);
        assertThat(second.importedCount()).isZero();
        assertThat(second.updatedCount()).isEqualTo(1);
        assertThat(lexemesByKey).hasSize(1);
    }

    @Test
    void importFromSangraha_sameSlp1DifferentGender_assignsDistinctMeaningNumbers() {
        LemmaExportItem tadMasculine = item("tad", "tad", "तद्", "MASCULINE", "PRONOUN", 10,
                List.of(), "тот", "that", null);
        LemmaExportItem tadNeuter = item("tad", "tad", "तद्", "NEUTER", "PRONOUN", 8,
                List.of(), "то", "that", null);
        SangrahaExportClient client = mock(SangrahaExportClient.class);
        when(client.fetchLemmaExport(any(), anyInt()))
                .thenReturn(new LemmaExportPage(List.of(tadMasculine, tadNeuter), null));

        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        java.util.Map<String, Lexeme> lexemesByKey = new java.util.HashMap<>();
        java.util.Map<String, Integer> maxMeaningBySlp1 = new java.util.HashMap<>();
        when(lexemeRepo.findByLemmaSlp1AndGender(any(), any())).thenAnswer(inv -> {
            String slp1 = inv.getArgument(0);
            LexemeGender gender = inv.getArgument(1);
            return Optional.ofNullable(lexemesByKey.get(slp1 + "|" + gender));
        });
        when(lexemeRepo.findMaxMeaningNumber(any())).thenAnswer(inv ->
                maxMeaningBySlp1.getOrDefault(inv.getArgument(0), 0));
        when(lexemeRepo.save(any())).thenAnswer(inv -> {
            Lexeme l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(UUID.randomUUID());
            }
            lexemesByKey.put(l.getLemmaSlp1() + "|" + l.getGender(), l);
            maxMeaningBySlp1.merge(l.getLemmaSlp1(), l.getMeaningNumber(), Math::max);
            return l;
        });
        when(lexemeRepo.count()).thenAnswer(inv -> (long) lexemesByKey.size());

        LexemeFrequencyRepository freqRepo = mock(LexemeFrequencyRepository.class);
        when(freqRepo.findById(any())).thenReturn(Optional.empty());
        when(freqRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PartOfSpeechRepository posRepo = mock(PartOfSpeechRepository.class);
        PartOfSpeech pronoun = new PartOfSpeech();
        pronoun.setCode("pronoun");
        pronoun.setGroup(PosGroup.NOMINAL);
        when(posRepo.findByCode("pronoun")).thenReturn(Optional.of(pronoun));

        MorphologyClassRepository morphologyRepo = mock(MorphologyClassRepository.class);
        when(morphologyRepo.findByCode(any())).thenReturn(Optional.empty());

        SemanticClassRepository semanticClassRepo = mock(SemanticClassRepository.class);

        LexiconImportService importService =
                service(client, lexemeRepo, freqRepo, posRepo, morphologyRepo, semanticClassRepo);

        SangrahaImportResult result = importService.importFromSangraha();

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(lexemesByKey.values()).extracting(Lexeme::getMeaningNumber)
                .containsExactlyInAnyOrder(1, 2);
    }
}