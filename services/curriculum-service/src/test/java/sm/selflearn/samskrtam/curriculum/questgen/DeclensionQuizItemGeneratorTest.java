package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.content.model.CaseType;
import sm.selflearn.samskrtam.content.model.NumberType;
import sm.selflearn.samskrtam.content.model.VowelType;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.paradigm.ParadigmForm;
import sm.selflearn.samskrtam.curriculum.paradigm.ParadigmFormRepository;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeclensionQuizItemGeneratorTest {

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private LexemeRepository lexemeRepository;
    private QuestItemRepository questItemRepository;
    private ParadigmFormRepository paradigmFormRepository;
    private ObjectMapper objectMapper;
    private DeclensionQuizItemGenerator generator;

    @BeforeEach
    void setUp() {
        lexemeRepository = mock(LexemeRepository.class);
        questItemRepository = mock(QuestItemRepository.class);
        paradigmFormRepository = mock(ParadigmFormRepository.class);
        objectMapper = new ObjectMapper();

        DeclensionMatchProperties properties = new DeclensionMatchProperties();
        properties.setPairsPerItem(5);

        generator = new DeclensionQuizItemGenerator(
                lexemeRepository, questItemRepository, paradigmFormRepository, properties, objectMapper);
    }

    private Topic topic(String code) {
        Topic topic = new Topic();
        topic.setId(TOPIC_ID);
        topic.setCode(code);
        return topic;
    }

    private Lexeme lexeme(String iast, String deva, LexemeGender gender, String... classCodes) {
        Lexeme l = new Lexeme();
        l.setId(UUID.randomUUID());
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

    /** Full 24-cell paradigm (8 cases x 3 numbers), forms taken from a-stem-masc canonical endings. */
    private List<ParadigmForm> aStemParadigm(String lemmaIast) {
        List<ParadigmForm> forms = new ArrayList<>();
        final String lemma = lemmaIast;
        for (CaseType caseType : CaseType.values()) {
            for (NumberType numberType : NumberType.values()) {
                ParadigmForm f = new ParadigmForm();
                f.setLemmaIast(lemma);
                f.setVowelType(VowelType.A_STEM);
                f.setCaseType(caseType);
                f.setNumberType(numberType);
                f.setFormIast(formIast(caseType, numberType));
                f.setFormDevanagari("नर");
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
            case NOMINATIVE -> "naraḥ";
            case ACCUSATIVE -> "naram";
            case INSTRUMENTAL -> "narena";
            case DATIVE -> "narāya";
            case ABLATIVE -> "narāt";
            case GENITIVE -> "narasya";
            case LOCATIVE -> "nare";
            case VOCATIVE -> "nara";
        };
    }

    /** Stubs the stored paradigm of an a-stem lemma. */
    private void stubParadigm(Lexeme lexeme) {
        when(paradigmFormRepository.findByLemmaIastAndVowelType(lexeme.getLemmaIast(), VowelType.A_STEM))
                .thenReturn(aStemParadigm(lexeme.getLemmaIast()));
    }

    @Test
    void supportedDomain_returnsGrammar() {
        assertThat(generator.isDomainSupported(TopicDomain.NOMINAL_MORPHOLOGY)).isTrue();
    }

    @Test
    void generate_aStemTopic_createsAllFourTypes() {
        Lexeme nara = lexeme("nara", "नर", LexemeGender.MASCULINE, "a-stem-masc");
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection()))
                .thenReturn(List.of(nara));
        stubParadigm(nara);
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
        verify(lexemeRepository, never()).findWithMorphologyByCodeIn(anyCollection());
        verify(questItemRepository, never()).saveAll(anyList());
    }

    @Test
    void generate_noLexemes_noop() {
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection())).thenReturn(List.of());

        assertThat(generator.generate(topic("a-stem"))).isZero();
        verify(questItemRepository, never()).saveAll(anyList());
    }

    @Test
    void generate_aStemMasculine_usesStoredForms() throws Exception {
        Lexeme nara = lexeme("nara", "नर", LexemeGender.MASCULINE, "a-stem-masc");
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection()))
                .thenReturn(List.of(nara));
        stubParadigm(nara);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        generator.generate(topic("a-stem"));

        List<QuestItem> saved = lastSavedItems();
        Map<String, DeclensionFormPayload> payloadsByCell = saved.stream()
                .filter(item -> item.getItemType().equals("DECLENSION_FORM"))
                .map(item -> read(item.getPayload(), DeclensionFormPayload.class))
                .collect(Collectors.toMap(
                        p -> p.caseType() + ":" + p.numberType(), p -> p));

        assertThat(payloadsByCell.get("NOMINATIVE:SINGULAR").correctFormIast()).isEqualTo("naraḥ");
        assertThat(payloadsByCell.get("ACCUSATIVE:SINGULAR").correctFormIast()).isEqualTo("naram");
        assertThat(payloadsByCell.get("INSTRUMENTAL:SINGULAR").correctFormIast()).isEqualTo("narena");
        assertThat(payloadsByCell.get("DATIVE:SINGULAR").correctFormIast()).isEqualTo("narāya");
        assertThat(payloadsByCell.get("GENITIVE:SINGULAR").correctFormIast()).isEqualTo("narasya");
        assertThat(payloadsByCell.get("LOCATIVE:SINGULAR").correctFormIast()).isEqualTo("nare");
        assertThat(payloadsByCell.get("VOCATIVE:SINGULAR").correctFormIast()).isEqualTo("nara");

        assertThat(payloadsByCell.get("NOMINATIVE:SINGULAR").correctFormDevanagari()).isEqualTo("नर");
        assertThat(payloadsByCell.get("NOMINATIVE:SINGULAR").highlights())
                .extracting(h -> h.text()).containsExactly("nara");

        QuestItem form = saved.stream()
                .filter(item -> item.getItemType().equals("DECLENSION_FORM"))
                .findFirst().orElseThrow();
        assertThat(form.getPrompt()).isEqualTo("Enter the correct nominative singular form of nara (नर).");
        assertThat(form.getPromptRu()).isEqualTo(
                "Введите правильную форму именительного падежа, единственного числа слова nara (नर).");
    }

    @Test
    void generate_setsProgressTagOnAllItems() throws Exception {
        Lexeme nara = lexeme("nara", "नर", LexemeGender.MASCULINE, "a-stem-masc");
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection()))
                .thenReturn(List.of(nara));
        stubParadigm(nara);
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
        // Two i-stem lemmas whose instrumental singular is the shared form "agninā";
        // nominative singular differs, so only the shared form needs gender disambiguation.
        Lexeme agniMasc = lexeme("agni", "अग्नि", LexemeGender.MASCULINE, "i-stem");
        Lexeme variNeut = lexeme("vāri", "वारि", LexemeGender.NEUTER, "i-stem");
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection()))
                .thenReturn(List.of(agniMasc, variNeut));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("agni", VowelType.I_STEM))
                .thenReturn(iStemParadigm("agni", "agniḥ"));
        when(paradigmFormRepository.findByLemmaIastAndVowelType("vāri", VowelType.I_STEM))
                .thenReturn(iStemParadigm("vāri", "vāri"));
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
    void generate_moreThanTenLexemes_randomSampleLimited() {
        List<Lexeme> many = java.util.stream.IntStream.range(0, 12)
                .mapToObj(i -> lexeme("nara" + i, "नर", LexemeGender.MASCULINE, "a-stem-masc"))
                .toList();
        many.forEach(this::stubParadigm);
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection())).thenReturn(many);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<QuestItem> items = invocation.getArgument(0);
            items.forEach(item -> item.setId(UUID.randomUUID()));
            return items;
        });

        // 10 lexemes * (24 + 24 + 24 + 5) items each
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
                        ? "agninā"
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