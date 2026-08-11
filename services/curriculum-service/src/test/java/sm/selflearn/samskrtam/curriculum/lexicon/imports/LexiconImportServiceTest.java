package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeFrequency;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeFrequencyId;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeStatus;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyAppliesTo;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PosGroup;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Source;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SourceKind;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SourceOccurrence;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SourceOccurrenceRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SourceRepository;
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

    private static VerseWordExportItem item(String work, String chapter, int order,
                                            String lemma, String stem, String surface,
                                            String pos, String glossRu, String gender) {
        return new VerseWordExportItem(
                UUID.randomUUID(), work, chapter, order,
                lemma, stem, surface, surface, pos, glossRu, "gloss-en", gender, null);
    }

    private LexiconImportService service(
            SangrahaExportClient client,
            LexemeRepository lexemeRepo,
            LexemeFrequencyRepository freqRepo,
            PartOfSpeechRepository posRepo,
            MorphologyClassRepository morphologyRepo,
            SourceRepository sourceRepo,
            SourceOccurrenceRepository occurrenceRepo) {
        return new LexiconImportService(
                client, new TransliterationService(),
                lexemeRepo, freqRepo, posRepo, morphologyRepo, sourceRepo, occurrenceRepo);
    }

    @Test
    void importFromSangraha_groupsByLemmaGender_andComputesRank() {
        VerseWordExportItem nara1 = item("gita", "ch1", 1, "nara", "nara", "naraḥ", "NOUN", "мужчина", "MASCULINE");
        VerseWordExportItem nara2 = item("gita", "ch1", 2, "nara", "nara", "naram", "NOUN", "мужчина", "MASCULINE");
        VerseWordExportItem jala = item("gita", "ch1", 1, "jala", "jala", "jalam", "NOUN", "вода", "NEUTER");
        SangrahaExportClient client = mock(SangrahaExportClient.class);
        when(client.fetchPage(any(), anyInt()))
                .thenReturn(new VerseWordExportPage(List.of(nara1, nara2, jala), null));

        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        when(lexemeRepo.findByLemmaSlp1AndGender(any(), any())).thenReturn(Optional.empty());

        PartOfSpeech pos = new PartOfSpeech();
        pos.setCode("noun");
        pos.setGroup(PosGroup.NOMINAL);
        PartOfSpeechRepository posRepo = mock(PartOfSpeechRepository.class);
        when(posRepo.findByCode("noun")).thenReturn(Optional.of(pos));

        MorphologyClass morphology = new MorphologyClass();
        morphology.setCode("a-stem-masc");
        morphology.setAppliesTo(MorphologyAppliesTo.NOUN);
        MorphologyClassRepository morphologyRepo = mock(MorphologyClassRepository.class);
        when(morphologyRepo.findByCode("a-stem-masc")).thenReturn(Optional.of(morphology));
        when(morphologyRepo.findByCode("a-stem-neut")).thenReturn(Optional.of(new MorphologyClass()));

        SourceRepository sourceRepo = mock(SourceRepository.class);
        java.util.Map<String, Source> sourcesBySlug = new java.util.HashMap<>();
        when(sourceRepo.findByExternalSangrahaWorkSlug(any()))
                .thenAnswer(invocation -> Optional.ofNullable(sourcesBySlug.get(invocation.getArgument(0))));
        when(sourceRepo.save(any())).thenAnswer(invocation -> {
            Source s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            sourcesBySlug.put(s.getExternalSangrahaWorkSlug(), s);
            return s;
        });

        LexemeFrequencyRepository freqRepo = mock(LexemeFrequencyRepository.class);
        when(freqRepo.findById(any())).thenReturn(Optional.empty());
        when(freqRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SourceOccurrenceRepository occurrenceRepo = mock(SourceOccurrenceRepository.class);
        when(occurrenceRepo.findBySourceIdAndLocationRef(any(), any())).thenReturn(List.of());
        when(occurrenceRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        when(lexemeRepo.save(any())).thenAnswer(invocation -> {
            Lexeme l = invocation.getArgument(0);
            if (l.getId() == null) {
                l.setId(UUID.randomUUID());
            }
            return l;
        });
        when(lexemeRepo.count()).thenReturn(2L);

        LexiconImportService importService =
                service(client, lexemeRepo, freqRepo, posRepo, morphologyRepo, sourceRepo, occurrenceRepo);

        SangrahaImportResult result = importService.importFromSangraha();

        assertThat(result.importedCount()).isEqualTo(2);
        assertThat(result.updatedCount()).isZero();
        assertThat(result.totalLexemeCount()).isEqualTo(2);
        assertThat(result.sourcesTouched()).isEqualTo(1);

        assertThat(lexemeRepo).isNotNull();
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
    void classifyStem_lastLetterFallback() {
        assertThat(LexiconImportService.classifyStem("rāma", LexemeGender.MASCULINE))
                .isEqualTo("a-stem-masc");
        assertThat(LexiconImportService.classifyStem("nārī", LexemeGender.FEMININE))
                .isEqualTo("i-stem");
        assertThat(LexiconImportService.classifyStem("guru", LexemeGender.MASCULINE))
                .isEqualTo("u-stem");
        assertThat(LexiconImportService.classifyStem(null, null)).isNull();
        assertThat(LexiconImportService.classifyStem("k", LexemeGender.MASCULINE)).isNull();
    }

    @Test
    void mapMorphology_vowelTypeTakesPriority() {
        assertThat(LexiconImportService.mapMorphology("A_STEM", "rāma", LexemeGender.MASCULINE))
                .isEqualTo("a-stem-masc");
        assertThat(LexiconImportService.mapMorphology("AA_STEM", "rāma", LexemeGender.FEMININE))
                .isEqualTo("a-stem-fem");
        assertThat(LexiconImportService.mapMorphology(null, "deva", LexemeGender.NEUTER))
                .isEqualTo("a-stem-neut");
    }

    @Test
    void pickRepresentative_mostFrequentAndSmallestVerseIdTieBreak() {
        VerseWordExportItem rare = item("gita", "ch1", 2, "nara", "naram", "naram", "NOUN", "мужчину", "MASCULINE");
        VerseWordExportItem mostFrequent = item("gita", "ch1", 9, "nara", "nara", "naram", "NOUN", "мужчина", "MASCULINE");
        VerseWordExportItem mostFrequentSecond = item("gita", "ch1", 7, "nara", "nara", "naram", "NOUN", "мужчина", "MASCULINE");
        VerseWordExportItem expected = mostFrequent.verseId().compareTo(mostFrequentSecond.verseId()) < 0
                ? mostFrequent : mostFrequentSecond;
        VerseWordExportItem representative = LexiconImportService.pickRepresentative(
                List.of(rare, mostFrequent, mostFrequentSecond));
        assertThat(representative.stem()).isEqualTo("nara");
        assertThat(representative.lemmaGlossRu()).isEqualTo("мужчина");
        assertThat(representative.verseId()).isEqualTo(expected.verseId());
    }

    @Test
    void importFromSangraha_secondRunOnSameData_createsNoDuplicates() {
        VerseWordExportItem nara = item("gita", "ch1", 1, "nara", "nara", "naraḥ", "NOUN", "мужчина", "MASCULINE");
        SangrahaExportClient client = mock(SangrahaExportClient.class);
        when(client.fetchPage(any(), anyInt()))
                .thenReturn(new VerseWordExportPage(List.of(nara), null));

        // Stateful in-memory lexemeRepo: dedup by (lemmaSlp1, gender).
        LexemeRepository lexemeRepo = mock(LexemeRepository.class);
        java.util.Map<String, Lexeme> lexemesByKey = new java.util.HashMap<>();
        when(lexemeRepo.findByLemmaSlp1AndGender(any(), any())).thenAnswer(invocation -> {
            String slp1 = invocation.getArgument(0);
            LexemeGender gender = invocation.getArgument(1);
            return Optional.ofNullable(lexemesByKey.get(slp1 + "|" + gender));
        });
        when(lexemeRepo.save(any())).thenAnswer(invocation -> {
            Lexeme l = invocation.getArgument(0);
            if (l.getId() == null) {
                l.setId(UUID.randomUUID());
            }
            lexemesByKey.put(l.getLemmaSlp1() + "|" + l.getGender(), l);
            return l;
        });
        when(lexemeRepo.count()).thenAnswer(invocation -> (long) lexemesByKey.size());

        // Stateful in-memory occurrenceRepo: dedup by (sourceId, locationRef).
        SourceOccurrenceRepository occurrenceRepo = mock(SourceOccurrenceRepository.class);
        java.util.Map<String, java.util.List<SourceOccurrence>> occBySource = new java.util.HashMap<>();
        when(occurrenceRepo.findBySourceIdAndLocationRef(any(), any())).thenAnswer(invocation -> {
            UUID sourceId = invocation.getArgument(0);
            String loc = invocation.getArgument(1);
            return occBySource.getOrDefault(sourceId + "|" + loc, List.of());
        });
        when(occurrenceRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(occurrenceRepo.countBySourceId(any())).thenReturn(1L);
        when(occurrenceRepo.countDistinctBySourceId(any())).thenReturn(1L);

        LexemeFrequencyRepository freqRepo = mock(LexemeFrequencyRepository.class);
        when(freqRepo.findById(any())).thenReturn(Optional.empty());
        when(freqRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PartOfSpeechRepository posRepo = mock(PartOfSpeechRepository.class);
        PartOfSpeech noun = new PartOfSpeech();
        noun.setCode("noun");
        noun.setGroup(PosGroup.NOMINAL);
        when(posRepo.findByCode("noun")).thenReturn(Optional.of(noun));

        MorphologyClassRepository morphologyRepo = mock(MorphologyClassRepository.class);
        when(morphologyRepo.findByCode(any())).thenAnswer(invocation -> {
            MorphologyClass mc = new MorphologyClass();
            mc.setCode(invocation.getArgument(0));
            return Optional.of(mc);
        });

        SourceRepository sourceRepo = mock(SourceRepository.class);
        java.util.Map<String, Source> sourcesBySlug = new java.util.HashMap<>();
        when(sourceRepo.findByExternalSangrahaWorkSlug(any()))
                .thenAnswer(invocation -> Optional.ofNullable(sourcesBySlug.get(invocation.getArgument(0))));
        when(sourceRepo.save(any())).thenAnswer(invocation -> {
            Source s = invocation.getArgument(0);
            if (s.getId() == null) {
                s.setId(UUID.randomUUID());
            }
            sourcesBySlug.put(s.getExternalSangrahaWorkSlug(), s);
            return s;
        });

        LexiconImportService importService =
                service(client, lexemeRepo, freqRepo, posRepo, morphologyRepo, sourceRepo, occurrenceRepo);

        SangrahaImportResult first = importService.importFromSangraha();
        SangrahaImportResult second = importService.importFromSangraha();

        assertThat(first.importedCount()).isEqualTo(1);
        assertThat(second.importedCount()).isZero();
        assertThat(second.updatedCount()).isEqualTo(1);
        assertThat(lexemesByKey).hasSize(1);
    }
}
