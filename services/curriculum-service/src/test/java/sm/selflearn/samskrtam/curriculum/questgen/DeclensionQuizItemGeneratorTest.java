package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.content.dto.frisch.FrischEntryDto;
import sm.selflearn.samskrtam.content.dto.frisch.FrischGenderDto;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.curriculum.dictionary.DictionaryClient;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.service.TransliterationService;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.paradigm.ParadigmForm;
import sm.selflearn.samskrtam.curriculum.paradigm.ParadigmFormRepository;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeclensionQuizItemGeneratorTest {

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private QuestItemRepository questItemRepository;
    private ParadigmFormRepository paradigmFormRepository;
    private DictionaryClient dictionaryClient;
    private TransliterationService transliterationService;
    private ObjectMapper objectMapper;
    private DeclensionQuizItemGenerator generator;

    @BeforeEach
    void setUp() {
        questItemRepository = mock(QuestItemRepository.class);
        paradigmFormRepository = mock(ParadigmFormRepository.class);
        dictionaryClient = mock(DictionaryClient.class);
        transliterationService = mock(TransliterationService.class);
        objectMapper = new ObjectMapper();

        DeclensionMatchProperties properties = new DeclensionMatchProperties();
        properties.setPairsPerItem(5);

        generator = new DeclensionQuizItemGenerator(
                questItemRepository, paradigmFormRepository, properties, objectMapper,
                dictionaryClient, transliterationService);

        when(dictionaryClient.getFrischLemma(any())).thenReturn(List.of());
        when(transliterationService.iastToDevanagari(anyString())).thenAnswer(a -> a.getArgument(0));
    }

    private Topic topic(String code) {
        Topic topic = new Topic();
        topic.setId(TOPIC_ID);
        topic.setCode(code);
        return topic;
    }

    private void stubFrischGender(String lemma, LexemeGender gender, String ru, String en) {
        FrischGenderDto g = gender == null ? null : new FrischGenderDto(gender.name(), null);
        FrischEntryDto entry = new FrischEntryDto(null, null,
                gender == null ? null : List.of(g),
                null, null, null, en, ru, lemma, null, null, null, null, null,
                null, null, null, null, null, null);
        when(dictionaryClient.getFrischLemma(lemma)).thenReturn(List.of(entry));
    }

    private void stubDistinctPairs(ParadigmFormRepository.LemmaVowelType... pairs) {
        when(paradigmFormRepository.findDistinctLemmaVowelTypeByVowelTypeIn(anyCollection()))
                .thenReturn(List.of(pairs));
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

    private List<ParadigmForm> aStemParadigm(String lemmaIast, VowelType vowelType) {
        List<ParadigmForm> forms = new ArrayList<>();
        for (CaseType caseType : CaseType.values()) {
            for (NumberType numberType : NumberType.values()) {
                ParadigmForm f = new ParadigmForm();
                f.setLemmaIast(lemmaIast);
                f.setVowelType(vowelType);
                f.setCaseType(caseType);
                f.setNumberType(numberType);
                f.setFormIast(formIast(caseType, numberType));
                f.setFormDevanagari("nara");
                forms.add(f);
            }
        }
        return forms;
    }

    private String formIast(CaseType caseType, NumberType numberType) {
        if (numberType != NumberType.SINGULAR) {
            return "nara-" + caseType.name().toLowerCase() + "-" + numberType.name().toLowerCase();
        }
        return switch (caseType) {
            case NOMINATIVE -> "narah";
            case ACCUSATIVE -> "naram";
            case INSTRUMENTAL -> "narena";
            case DATIVE -> "naraya";
            case ABLATIVE -> "narat";
            case GENITIVE -> "narasya";
            case LOCATIVE -> "nare";
            case VOCATIVE -> "nara";
        };
    }

    private void stubParadigm(String lemma, VowelType vowelType) {
        when(paradigmFormRepository.findByLemmaIastAndVowelType(lemma, vowelType))
                .thenReturn(aStemParadigm(lemma, vowelType));
    }

    @Test
    void supportedDomain_includesNounsAndPronouns() {
        assertThat(generator.isDomainSupported(TopicDomain.NOMINAL_MORPHOLOGY)).isTrue();
        assertThat(generator.isDomainSupported(TopicDomain.PRONOUNS)).isTrue();
    }

    @Test
    void generate_aStemTopic_createsAllFourTypes() {
        stubDistinctPairs(pair("nara", VowelType.A_STEM));
        stubParadigm("nara", VowelType.A_STEM);
        stubFrischGender("nara", LexemeGender.MASCULINE, "chelovek", "man");
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<QuestItem> items = invocation.getArgument(0);
            items.forEach(item -> item.setId(UUID.randomUUID()));
            return items;
        });

        // 24 cells -> 24 FORM + 24 FORM_CHOICE + 24 CASE_RECOGNITION + 5 MATCH blocks (ceil(24/5))
        assertThat(generator.generate(topic("a-stem"))).isEqualTo(77);

        verify(questItemRepository).saveAll(anyList());
    }

    @Test
    void generate_unknownTopic_noop() {
        assertThat(generator.generate(topic("class-1"))).isZero();
        verify(paradigmFormRepository, never()).findDistinctLemmaVowelTypeByVowelTypeIn(anyCollection());
        verify(questItemRepository, never()).saveAll(anyList());
    }

    @Test
    void generate_noStems_noop() {
        stubDistinctPairs();
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(generator.generate(topic("a-stem"))).isZero();
        verify(questItemRepository, never()).saveAll(anyList());
    }

    @Test
    void generate_aStemMasculine_usesStoredFormsAndFrisch() throws Exception {
        stubDistinctPairs(pair("nara", VowelType.A_STEM));
        stubParadigm("nara", VowelType.A_STEM);
        stubFrischGender("nara", LexemeGender.MASCULINE, "chelovek", "man");
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        generator.generate(topic("a-stem"));

        List<QuestItem> saved = lastSavedItems();
        Map<String, DeclensionFormPayload> payloadsByCell = saved.stream()
                .filter(item -> item.getItemType().equals("DECLENSION_FORM"))
                .map(item -> read(item.getPayload(), DeclensionFormPayload.class))
                .collect(Collectors.toMap(
                        p -> p.caseType() + ":" + p.numberType(), p -> p));

        assertThat(payloadsByCell.get("NOMINATIVE:SINGULAR").correctFormIast()).isEqualTo("narah");
        assertThat(payloadsByCell.get("ACCUSATIVE:SINGULAR").correctFormIast()).isEqualTo("naram");
        assertThat(payloadsByCell.get("INSTRUMENTAL:SINGULAR").correctFormIast()).isEqualTo("narena");
        assertThat(payloadsByCell.get("DATIVE:SINGULAR").correctFormIast()).isEqualTo("naraya");
        assertThat(payloadsByCell.get("GENITIVE:SINGULAR").correctFormIast()).isEqualTo("narasya");
        assertThat(payloadsByCell.get("LOCATIVE:SINGULAR").correctFormIast()).isEqualTo("nare");
        assertThat(payloadsByCell.get("VOCATIVE:SINGULAR").correctFormIast()).isEqualTo("nara");

        assertThat(payloadsByCell.get("NOMINATIVE:SINGULAR").correctFormDevanagari()).isEqualTo("nara");
        assertThat(payloadsByCell.get("NOMINATIVE:SINGULAR").highlights())
                .extracting(h -> h.text()).containsExactly("nara");

        QuestItem form = saved.stream()
                .filter(item -> item.getItemType().equals("DECLENSION_FORM"))
                .findFirst().orElseThrow();
        assertThat(form.getPrompt()).contains("nara").contains("'man'");
        assertThat(form.getPromptRu()).contains("nara").contains("chelovek");
    }

    @Test
    void generate_setsProgressTagOnAllItems() throws Exception {
        stubDistinctPairs(pair("nara", VowelType.A_STEM));
        stubParadigm("nara", VowelType.A_STEM);
        stubFrischGender("nara", LexemeGender.MASCULINE, "chelovek", "man");
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        generator.generate(topic("a-stem"));

        List<QuestItem> saved = lastSavedItems();
        List<QuestItem> forms = saved.stream()
                .filter(item -> item.getItemType().equals("DECLENSION_FORM"))
                .toList();
        assertThat(forms).isNotEmpty();
        assertThat(forms).allMatch(item -> item.getProgressTag().endsWith("|MASCULINE"));
        assertThat(forms).anyMatch(item -> item.getProgressTag().equals("NOMINATIVE|SINGULAR|MASCULINE"));

        List<QuestItem> matches = saved.stream()
                .filter(item -> item.getItemType().equals("DECLENSION_MATCH"))
                .toList();
        assertThat(matches).isNotEmpty();
        assertThat(matches).allMatch(item -> item.getProgressTag().endsWith("|UNSPECIFIED"));
        assertThat(matches).anyMatch(item -> item.getProgressTag().startsWith("NOMINATIVE|SINGULAR|"));
    }

    @Test
    void generate_ambiguousFormAcrossGenders_genderRequiredTrue() throws Exception {
        stubDistinctPairs(pair("agni", VowelType.I_STEM), pair("vari", VowelType.I_STEM));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("agni", VowelType.I_STEM))
                .thenReturn(iStemParadigm("agni", "agnih"));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("vari", VowelType.I_STEM))
                .thenReturn(iStemParadigm("vari", "vari"));
        stubFrischGender("agni", LexemeGender.MASCULINE, "ogon", "fire");
        stubFrischGender("vari", LexemeGender.NEUTER, "voda", "water");
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        generator.generate(topic("i-u-stems"));

        List<CaseRecognitionPayload> caseItems = lastSavedItems().stream()
                .filter(item -> item.getItemType().equals("CASE_RECOGNITION"))
                .map(item -> read(item.getPayload(), CaseRecognitionPayload.class))
                .toList();

        List<CaseRecognitionPayload> instrumental = caseItems.stream()
                .filter(p -> p.correctCaseType().equals("INSTRUMENTAL"))
                .filter(p -> p.correctNumberType().equals("SINGULAR"))
                .toList();
        assertThat(instrumental).hasSize(2);
        assertThat(instrumental).allMatch(CaseRecognitionPayload::genderRequired);

        List<CaseRecognitionPayload> nominative = caseItems.stream()
                .filter(p -> p.correctCaseType().equals("NOMINATIVE"))
                .filter(p -> p.correctNumberType().equals("SINGULAR"))
                .toList();
        assertThat(nominative).hasSize(2);
        assertThat(nominative).noneMatch(CaseRecognitionPayload::genderRequired);
    }

    @Test
    void generate_moreThanTenLemmas_randomSampleLimited() {
        List<String> many = java.util.stream.IntStream.range(0, 12)
                .mapToObj(i -> "nara" + i)
                .toList();
        ParadigmFormRepository.LemmaVowelType[] pairs = many.stream()
                .map(lemma -> pair(lemma, VowelType.A_STEM))
                .toArray(ParadigmFormRepository.LemmaVowelType[]::new);
        stubDistinctPairs(pairs);
        for (String lemma : many) {
            stubParadigm(lemma, VowelType.A_STEM);
            stubFrischGender(lemma, LexemeGender.MASCULINE, "chelovek", "man");
        }
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<QuestItem> items = invocation.getArgument(0);
            items.forEach(item -> item.setId(UUID.randomUUID()));
            return items;
        });

        // 10 lemmas * (24 + 24 + 24 + 5) items each
        assertThat(generator.generate(topic("a-stem"))).isEqualTo(10 * 77);
    }

    /** i-stem paradigm: instrumental singular identical across lemmas, nominative singular differs. */
    private List<ParadigmForm> iStemParadigm(String lemmaIast, String nominativeSingular) {
        List<ParadigmForm> forms = new ArrayList<>();
        for (CaseType caseType : CaseType.values()) {
            for (NumberType numberType : NumberType.values()) {
                ParadigmForm f = new ParadigmForm();
                f.setLemmaIast(lemmaIast);
                f.setVowelType(VowelType.I_STEM);
                f.setCaseType(caseType);
                f.setNumberType(numberType);
                String form = (caseType == CaseType.INSTRUMENTAL && numberType == NumberType.SINGULAR)
                        ? "agnina"
                        : (caseType == CaseType.NOMINATIVE && numberType == NumberType.SINGULAR)
                                ? nominativeSingular
                                : "agni-" + caseType.name().toLowerCase() + "-" + numberType.name().toLowerCase();
                f.setFormIast(form);
                forms.add(f);
            }
        }
        return forms;
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private List<QuestItem> lastSavedItems() {
        var captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(questItemRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
