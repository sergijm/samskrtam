package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PosGroup;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.service.TransliterationService;

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
                                        String gender, String posCode, int occCount,
                                        String glossRu, String glossEn, String vowelType) {
        return new LemmaExportItem(UUID.randomUUID(), slp1, iast, devanagari,
                gender, posCode, occCount, null, glossRu, glossEn, vowelType);
    }

    private LexiconImportService service(
            SangrahaExportClient client,
            LexemeRepository lexemeRepo,
            LexemeFrequencyRepository freqRepo,
            PartOfSpeechRepository posRepo,
            MorphologyClassRepository morphologyRepo,
            SemanticTopicRepository semanticTopicRepo) {
        return new LexiconImportService(
                client, new TransliterationService(),
                lexemeRepo, freqRepo, posRepo, morphologyRepo, semanticTopicRepo);
    }

    @Test
    void importFromSangraha_simpleUpsertFromPreAggregatedData() {
        LemmaExportItem nara = item("nara", "nara", "नर", "MASCULINE", "noun", 42, "мужчина", "man", "A_STEM");
        LemmaExportItem jala = item("jala", "jala", "जल", "NEUTER", "noun", 15, "вода", "water", "A_STEM");

        SangrahaExportClient client = mock(SangrahaExportClient.class);
        when(client.fetchLemmaExport(any(), anyInt()))
                .thenReturn(new LemmaExportPage(List.of(nara, jala), null));

        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        when(lexemeRepo.findByLemmaSlp1AndGender(any(), any())).thenReturn(Optional.empty());
        when(lexemeRepo.save(any())).thenAnswer(inv -> {
            Lexeme l = inv.getArgument(0);
            if (l.getId() == null) l.setId(UUID.randomUUID());
            return l;
        });
        when(lexemeRepo.count()).thenReturn(2L);

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

        SemanticTopicRepository semanticTopicRepo = mock(SemanticTopicRepository.class);

        LexiconImportService importService = service(client, lexemeRepo, freqRepo, posRepo,
                morphologyRepo, semanticTopicRepo);

        SangrahaImportResult result = importService.importFromSangraha();

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.totalLexemeCount()).isEqualTo(2);
    }

    @Test
    void importFromSangraha_secondRunOnSameData_createsNoDuplicates() {
        LemmaExportItem nara = item("nara", "nara", "नर", "MASCULINE", "noun", 42, "мужчина", "man", "A_STEM");

        SangrahaExportClient client = mock(SangrahaExportClient.class);
        when(client.fetchLemmaExport(any(), anyInt()))
                .thenReturn(new LemmaExportPage(List.of(nara), null));

        java.util.Map<String, Lexeme> lexemesByKey = new java.util.HashMap<>();
        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        when(lexemeRepo.findByLemmaSlp1AndGender(any(), any())).thenAnswer(inv -> {
            String slp1 = inv.getArgument(0);
            LexemeGender gender = inv.getArgument(1);
            return Optional.ofNullable(lexemesByKey.get(slp1 + "|" + gender));
        });
        when(lexemeRepo.save(any())).thenAnswer(inv -> {
            Lexeme l = inv.getArgument(0);
            if (l.getId() == null) l.setId(UUID.randomUUID());
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

        SemanticTopicRepository semanticTopicRepo = mock(SemanticTopicRepository.class);

        LexiconImportService importService = service(client, lexemeRepo, freqRepo, posRepo,
                morphologyRepo, semanticTopicRepo);

        SangrahaImportResult first = importService.importFromSangraha();
        SangrahaImportResult second = importService.importFromSangraha();

        assertThat(first.importedCount()).isEqualTo(1);
        assertThat(second.importedCount()).isZero();
        assertThat(second.updatedCount()).isEqualTo(1);
        assertThat(lexemesByKey).hasSize(1);
    }

    @Test
    void mapMorphologyCode_vowelTypeMapping() {
        assertThat(LexiconImportService.mapMorphologyCode("MASCULINE", "A_STEM")).isEqualTo("a-stem-masc");
        assertThat(LexiconImportService.mapMorphologyCode("NEUTER", "A_STEM")).isEqualTo("a-stem-neut");
        assertThat(LexiconImportService.mapMorphologyCode("FEMININE", "AA_STEM")).isEqualTo("a-stem-fem");
        assertThat(LexiconImportService.mapMorphologyCode(null, "I_STEM")).isEqualTo("i-stem");
        assertThat(LexiconImportService.mapMorphologyCode(null, "U_STEM")).isEqualTo("u-stem");
        assertThat(LexiconImportService.mapMorphologyCode(null, "R_STEM")).isEqualTo("r-stem");
        assertThat(LexiconImportService.mapMorphologyCode(null, null)).isNull();
        assertThat(LexiconImportService.mapMorphologyCode(null, "UNKNOWN")).isNull();
    }
}