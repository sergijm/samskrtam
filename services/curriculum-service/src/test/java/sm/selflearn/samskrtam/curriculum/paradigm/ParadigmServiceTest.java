package sm.selflearn.samskrtam.curriculum.paradigm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.content.dto.DeclensionParadigmPageDto;
import sm.selflearn.samskrtam.content.dto.frisch.FrischEntryDto;
import sm.selflearn.samskrtam.content.dto.frisch.FrischGenderDto;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.morphology.Gender;
import sm.selflearn.samskrtam.morphology.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.curriculum.dictionary.DictionaryClient;
import sm.selflearn.samskrtam.curriculum.lexicon.service.TransliterationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParadigmServiceTest {

    private ParadigmFormRepository paradigmFormRepository;
    private DictionaryClient dictionaryClient;
    private TransliterationService transliterationService;
    private ParadigmService service;

    @BeforeEach
    void setUp() {
        paradigmFormRepository = mock(ParadigmFormRepository.class);
        dictionaryClient = mock(DictionaryClient.class);
        transliterationService = mock(TransliterationService.class);
        when(dictionaryClient.getFrischLemma(any())).thenReturn(List.of());
        when(transliterationService.iastToDevanagari(anyString())).thenAnswer(a -> a.getArgument(0));
        service = new ParadigmService(paradigmFormRepository, dictionaryClient, transliterationService);
    }

    private void stubFrischGender(String lemma, Gender gender, String ru, String en) {
        FrischGenderDto g = gender == null ? null : new FrischGenderDto(gender.name(), null);
        FrischEntryDto entry = new FrischEntryDto(null, null,
                gender == null ? null : List.of(g),
                null, null, null, en, ru, lemma, null, null, null, null, null,
                null, null, null, null, null, null);
        when(dictionaryClient.getFrischLemma(lemma)).thenReturn(List.of(entry));
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

    private ParadigmFormRepository.LemmaVowelType pair(String lemmaIast, VowelType vowelType) {
        return new ParadigmFormRepository.LemmaVowelType() {
            public String getLemmaIast() {
                return lemmaIast;
            }

            public VowelType getVowelType() {
                return vowelType;
            }
        };
    }

    private void stubPairs(List<VowelType> vowelTypes, ParadigmFormRepository.LemmaVowelType... pairs) {
        when(paradigmFormRepository.findDistinctLemmaVowelTypeByVowelTypeIn(anyCollection()))
                .thenReturn(List.of(pairs));
    }

    private void stubForms(String lemmaIast, VowelType vowelType, String nominativeSingular) {
        when(paradigmFormRepository.findByLemmaIastAndVowelType(lemmaIast, vowelType))
                .thenReturn(List.of(form(lemmaIast, vowelType,
                        CaseType.NOMINATIVE, NumberType.SINGULAR, nominativeSingular)));
    }

    @Test
    void aStem_masc_usesFrischGender() {
        stubPairs(List.of(VowelType.A_STEM), pair("nara", VowelType.A_STEM));
        stubForms("nara", VowelType.A_STEM, "naraḥ");
        stubFrischGender("nara", Gender.MASCULINE, "человек", "man");

        DeclensionParadigmPageDto page = service.getParadigmPage("a-stem", 0);

        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getVowelType()).isEqualTo(VowelType.A_STEM);
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.MASCULINE);
        assertThat(page.getParadigm().getTranslationRu()).isEqualTo("человек");
        assertThat(page.getParadigm().getForms()).isNotEmpty();
        assertThat(page.getParadigm().getForms())
                .anyMatch(f -> f.getCaseType().name().equals("NOMINATIVE")
                        && f.getNumberType().name().equals("SINGULAR")
                        && f.getFormIast().equals("naraḥ"));
    }

    @Test
    void aStem_neuter_usesFrischGender() {
        stubPairs(List.of(VowelType.A_STEM), pair("phala", VowelType.A_STEM));
        stubForms("phala", VowelType.A_STEM, "phalam");
        stubFrischGender("phala", Gender.NEUTER, "плод", "fruit");

        DeclensionParadigmPageDto page = service.getParadigmPage("a-stem", 0);

        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getVowelType()).isEqualTo(VowelType.A_STEM);
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.NEUTER);
        assertThat(page.getParadigm().getTranslationEn()).isEqualTo("fruit");
    }

    @Test
    void multipleLemmas_sortedAlphabetically() {
        stubPairs(List.of(VowelType.A_STEM), pair("nara", VowelType.A_STEM), pair("phala", VowelType.A_STEM));
        stubForms("nara", VowelType.A_STEM, "naraḥ");
        stubForms("phala", VowelType.A_STEM, "phalam");
        stubFrischGender("nara", Gender.MASCULINE, "человек", "man");
        stubFrischGender("phala", Gender.NEUTER, "плод", "fruit");

        DeclensionParadigmPageDto page = service.getParadigmPage("a-stem", 1);

        assertThat(page.getTotalCount()).isEqualTo(2);
        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getStemIast()).isEqualTo("phala");
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.NEUTER);
    }

    @Test
    void orderedByIastAscending() {
        stubPairs(List.of(VowelType.A_STEM), pair("hara", VowelType.A_STEM), pair("nara", VowelType.A_STEM));
        stubForms("hara", VowelType.A_STEM, "haraḥ");
        stubForms("nara", VowelType.A_STEM, "naraḥ");
        stubFrischGender("hara", Gender.MASCULINE, "брать", "to take");
        stubFrischGender("nara", Gender.MASCULINE, "человек", "man");

        DeclensionParadigmPageDto page0 = service.getParadigmPage("a-stem", 0);
        DeclensionParadigmPageDto page1 = service.getParadigmPage("a-stem", 1);

        assertThat(page0.getParadigm().getStemIast()).isEqualTo("hara");
        assertThat(page1.getParadigm().getStemIast()).isEqualTo("nara");
    }

    @Test
    void pronoun_frischWithoutGender_isUnspecified() {
        stubPairs(List.of(VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM,
                        VowelType.PRON_IDAM_MASC, VowelType.PRON_IDAM_NEUT, VowelType.PRON_IDAM_FEM,
                        VowelType.PRON_ADAS_MASC, VowelType.PRON_ADAS_NEUT, VowelType.PRON_ADAS_FEM),
                pair("tad", VowelType.PRON_TAD_MASC));
        stubForms("tad", VowelType.PRON_TAD_MASC, "saḥ");
        stubFrischGender("tad", null, "тот", "that");

        DeclensionParadigmPageDto page = service.getParadigmPage("demonstrative-pronouns", 0);

        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getStemIast()).isEqualTo("tad");
        assertThat(page.getParadigm().getVowelType()).isEqualTo(VowelType.PRON_TAD_MASC);
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.UNSPECIFIED);
    }

    @Test
    void pronoun_emptyLemmaSearch_fallsBackToNominativeSingular() {
        stubPairs(List.of(VowelType.PRON_TAD_MASC, VowelType.PRON_TAD_NEUT, VowelType.PRON_TAD_FEM,
                        VowelType.PRON_IDAM_MASC, VowelType.PRON_IDAM_NEUT, VowelType.PRON_IDAM_FEM,
                        VowelType.PRON_ADAS_MASC, VowelType.PRON_ADAS_NEUT, VowelType.PRON_ADAS_FEM),
                pair("tad", VowelType.PRON_TAD_MASC));
        stubForms("tad", VowelType.PRON_TAD_MASC, "saḥ");
        // базовая лемма "tad" в Фрише не найдена -> фоллбэк по форме "saḥ"
        stubFrischGender("saḥ", Gender.MASCULINE, "он", "he");

        DeclensionParadigmPageDto page = service.getParadigmPage("demonstrative-pronouns", 0);

        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getParadigm()).isNotNull();
        assertThat(page.getParadigm().getStemIast()).isEqualTo("tad");
        assertThat(page.getParadigm().getGender()).isEqualTo(Gender.MASCULINE);
        assertThat(page.getParadigm().getTranslationRu()).isEqualTo("он");
    }

    @Test
    void multiClassTopic_iUStems_servesParadigmsOfBothVowelTypes() {
        stubPairs(List.of(VowelType.I_STEM, VowelType.U_STEM),
                pair("agni", VowelType.I_STEM), pair("vedu", VowelType.U_STEM));
        stubForms("agni", VowelType.I_STEM, "agniḥ");
        stubForms("vedu", VowelType.U_STEM, "veduḥ");
        stubFrischGender("agni", Gender.MASCULINE, "огонь", "fire");
        stubFrischGender("vedu", Gender.MASCULINE, "веда", "veda");

        DeclensionParadigmPageDto iPage = service.getParadigmPage("i-u-stems", 0);
        DeclensionParadigmPageDto uPage = service.getParadigmPage("i-u-stems", 1);

        assertThat(iPage.getTotalCount()).isEqualTo(2);
        assertThat(iPage.getParadigm().getStemIast()).isEqualTo("agni");
        assertThat(iPage.getParadigm().getVowelType()).isEqualTo(VowelType.I_STEM);
        assertThat(uPage.getParadigm().getStemIast()).isEqualTo("vedu");
        assertThat(uPage.getParadigm().getVowelType()).isEqualTo(VowelType.U_STEM);
    }
}
