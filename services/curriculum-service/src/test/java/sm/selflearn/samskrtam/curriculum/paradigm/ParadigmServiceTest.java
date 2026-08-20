package sm.selflearn.samskrtam.curriculum.paradigm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.Gender;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeFrequency;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeFrequencyId;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParadigmServiceTest {

    private LexemeRepository lexemeRepository;
    private LexemeFrequencyRepository lexemeFrequencyRepository;
    private ParadigmFormRepository paradigmFormRepository;
    private ParadigmService service;

    @BeforeEach
    void setUp() {
        lexemeRepository = mock(LexemeRepository.class);
        lexemeFrequencyRepository = mock(LexemeFrequencyRepository.class);
        paradigmFormRepository = mock(ParadigmFormRepository.class);
        service = new ParadigmService(paradigmFormRepository, lexemeRepository, lexemeFrequencyRepository);
    }

    private Lexeme lexeme(UUID id, String iast, String deva, LexemeGender gender, String... classCodes) {
        Lexeme l = new Lexeme();
        l.setId(id);
        l.setLemmaIast(iast);
        l.setLemmaDevanagari(deva);
        l.setGender(gender);
        Set<MorphologyClass> classes = new HashSet<>();
        for (String code : classCodes) {
            MorphologyClass mc = new MorphologyClass();
            mc.setCode(code);
            classes.add(mc);
        }
        l.setMorphologyClasses(classes);
        return l;
    }

    private ParadigmForm form(String lemmaIast, VowelType vowelType, CaseType caseType, NumberType numberType, String iast) {
        ParadigmForm f = new ParadigmForm();
        f.setLemmaIast(lemmaIast);
        f.setVowelType(vowelType);
        f.setCaseType(caseType);
        f.setNumberType(numberType);
        f.setFormIast(iast);
        return f;
    }

    private void stubForms(List<VowelType> vowelTypes, String lemmaIast, VowelType vowelType,
                           String nominativeSingular) {
        when(paradigmFormRepository.findDistinctLemmaIastsByVowelTypeIn(anyCollection()))
                .thenReturn(List.of(lemmaIast));
        when(paradigmFormRepository.findByLemmaIastAndVowelType(lemmaIast, vowelType))
                .thenReturn(List.of(form(lemmaIast, vowelType,
                        CaseType.NOMINATIVE, NumberType.SINGULAR, nominativeSingular)));
    }

    @Test
    void aStemMerged_lexemeGenderNull_usesActualClass() {
        Lexeme nara = lexeme(UUID.fromString("00000000-0000-0000-0000-000000000001"), "nara", "नर", null, "a-stem-masc");
        when(lexemeRepository.findNounsWithMorphologyByCodeIn(eq(List.of("a-stem-masc", "a-stem-neut"))))
                .thenReturn(List.of(nara));
        stubForms(List.of(VowelType.A_STEM), "nara", VowelType.A_STEM, "naraḥ");

        DeclensionParadigmPageDto page = service.getParadigmPage("a-stem", 0);

        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getVowelType()).isEqualTo(VowelType.A_STEM);
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.MASCULINE);
        assertThat(page.getParadigm().getForms()).isNotEmpty();
        assertThat(page.getParadigm().getForms())
                .anyMatch(f -> f.getCaseType().name().equals("NOMINATIVE")
                        && f.getNumberType().name().equals("SINGULAR")
                        && f.getFormIast().equals("naraḥ"));
    }

    @Test
    void aStemMerged_neuterLexemeGenderNull_usesActualClass() {
        Lexeme phala = lexeme(UUID.fromString("00000000-0000-0000-0000-000000000001"), "phala", "फल", null, "a-stem-neut");
        when(lexemeRepository.findNounsWithMorphologyByCodeIn(eq(List.of("a-stem-masc", "a-stem-neut"))))
                .thenReturn(List.of(phala));
        stubForms(List.of(VowelType.A_STEM), "phala", VowelType.A_STEM, "phalam");

        DeclensionParadigmPageDto page = service.getParadigmPage("a-stem", 0);

        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getVowelType()).isEqualTo(VowelType.A_STEM);
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.NEUTER);
        assertThat(page.getParadigm().getForms())
                .anyMatch(f -> f.getCaseType().name().equals("NOMINATIVE")
                        && f.getNumberType().name().equals("SINGULAR")
                        && f.getFormIast().equals("phalam"));
    }

    @Test
    void aStemMerged_allLexemesBoundBeforeParadigms() {
        Lexeme nara = lexeme(UUID.fromString("00000000-0000-0000-0000-000000000001"), "nara", "नर", LexemeGender.MASCULINE, "a-stem-masc");
        Lexeme phala = lexeme(UUID.fromString("00000000-0000-0000-0000-000000000002"), "phala", "फल", LexemeGender.NEUTER, "a-stem-neut");
        when(lexemeRepository.findNounsWithMorphologyByCodeIn(anyCollection()))
                .thenReturn(List.of(nara, phala));
        when(paradigmFormRepository.findDistinctLemmaIastsByVowelTypeIn(anyCollection()))
                .thenReturn(List.of("nara", "phala"));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("nara", VowelType.A_STEM))
                .thenReturn(List.of(form("nara", VowelType.A_STEM,
                        CaseType.NOMINATIVE, NumberType.SINGULAR, "naraḥ")));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("phala", VowelType.A_STEM))
                .thenReturn(List.of(form("phala", VowelType.A_STEM,
                        CaseType.NOMINATIVE, NumberType.SINGULAR, "phalam")));

        DeclensionParadigmPageDto page = service.getParadigmPage("a-stem", 1);

        assertThat(page.getTotalCount()).isEqualTo(2);
        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.NEUTER);
    }

    @Test
    void aStemMerged_orderedByFrequencyRankAscending() {
        UUID rareId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID commonId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Lexeme rare = lexeme(rareId, "hara", "हर", LexemeGender.MASCULINE, "a-stem-masc");
        Lexeme common = lexeme(commonId, "nara", "नर", LexemeGender.MASCULINE, "a-stem-masc");
        when(lexemeRepository.findNounsWithMorphologyByCodeIn(eq(List.of("a-stem-masc", "a-stem-neut"))))
                .thenReturn(List.of(rare, common));
        when(paradigmFormRepository.findDistinctLemmaIastsByVowelTypeIn(anyCollection()))
                .thenReturn(List.of("hara", "nara"));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("hara", VowelType.A_STEM))
                .thenReturn(List.of(form("hara", VowelType.A_STEM,
                        CaseType.NOMINATIVE, NumberType.SINGULAR, "haraḥ")));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("nara", VowelType.A_STEM))
                .thenReturn(List.of(form("nara", VowelType.A_STEM,
                        CaseType.NOMINATIVE, NumberType.SINGULAR, "naraḥ")));

        LexemeFrequency commonFreq = frequency(commonId, 1);
        LexemeFrequency rareFreq = frequency(rareId, 50);
        when(lexemeFrequencyRepository.findBySourceAndLexemeIdIn(eq("SANGRAHA_CORPUS"), anyCollection()))
                .thenReturn(List.of(commonFreq, rareFreq));

        DeclensionParadigmPageDto page0 = service.getParadigmPage("a-stem", 0);
        DeclensionParadigmPageDto page1 = service.getParadigmPage("a-stem", 1);

        assertThat(page0.getParadigm().getStemIast()).isEqualTo("nara");
        assertThat(page1.getParadigm().getStemIast()).isEqualTo("hara");
    }

    @Test
    void suppletive_pronounLexemesLoadedByLemma() {
        UUID tadId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Lexeme tad = lexeme(tadId, "tad", "तद्", LexemeGender.UNSPECIFIED);
        when(lexemeRepository.findByLemmaIastIn(eq(List.of("tad", "etad", "idam"))))
                .thenReturn(List.of(tad));
        when(paradigmFormRepository.findDistinctLemmaIastsByVowelTypeIn(anyCollection()))
                .thenReturn(List.of("tad"));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("tad", VowelType.PRON_TAD))
                .thenReturn(List.of(form("tad", VowelType.PRON_TAD,
                        CaseType.NOMINATIVE, NumberType.SINGULAR, "saḥ")));

        DeclensionParadigmPageDto page = service.getParadigmPage("demonstrative-pronouns", 0);

        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getStemIast()).isEqualTo("tad");
        assertThat(page.getParadigm().getVowelType()).isEqualTo(VowelType.PRON_TAD);
    }

    @Test
    void multiClassTopic_iUStems_servesParadigmsOfBothVowelTypes() {
        Lexeme agni = lexeme(UUID.fromString("00000000-0000-0000-0000-000000000001"), "agni", "अग्नि", LexemeGender.MASCULINE, "i-stem");
        Lexeme vedu = lexeme(UUID.fromString("00000000-0000-0000-0000-000000000002"), "vedu", "वेदु", LexemeGender.MASCULINE, "u-stem");
        when(lexemeRepository.findNounsWithMorphologyByCodeIn(eq(List.of("i-stem", "u-stem"))))
                .thenReturn(List.of(agni, vedu));
        when(paradigmFormRepository.findDistinctLemmaIastsByVowelTypeIn(anyCollection()))
                .thenReturn(List.of("agni", "vedu"));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("agni", VowelType.I_STEM))
                .thenReturn(List.of(form("agni", VowelType.I_STEM,
                        CaseType.NOMINATIVE, NumberType.SINGULAR, "agniḥ")));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("vedu", VowelType.U_STEM))
                .thenReturn(List.of(form("vedu", VowelType.U_STEM,
                        CaseType.NOMINATIVE, NumberType.SINGULAR, "veduḥ")));

        DeclensionParadigmPageDto iPage = service.getParadigmPage("i-u-stems", 0);
        DeclensionParadigmPageDto uPage = service.getParadigmPage("i-u-stems", 1);

        assertThat(iPage.getTotalCount()).isEqualTo(2);
        assertThat(iPage.getParadigm().getStemIast()).isEqualTo("agni");
        assertThat(iPage.getParadigm().getVowelType()).isEqualTo(VowelType.I_STEM);
        assertThat(uPage.getParadigm().getStemIast()).isEqualTo("vedu");
        assertThat(uPage.getParadigm().getVowelType()).isEqualTo(VowelType.U_STEM);
    }

    private LexemeFrequency frequency(UUID lexemeId, int rank) {
        LexemeFrequencyId id = new LexemeFrequencyId();
        id.setLexemeId(lexemeId);
        id.setSource("SANGRAHA_CORPUS");
        LexemeFrequency f = new LexemeFrequency();
        f.setId(id);
        f.setRank(rank);
        return f;
    }
}