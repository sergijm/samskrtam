package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeclensionQuizItemGeneratorTest {

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private LexemeRepository lexemeRepository;
    private QuestItemRepository questItemRepository;
    private ObjectMapper objectMapper;
    private DeclensionQuizItemGenerator generator;

    @BeforeEach
    void setUp() {
        lexemeRepository = mock(LexemeRepository.class);
        questItemRepository = mock(QuestItemRepository.class);
        objectMapper = new ObjectMapper();

        DeclensionMatchProperties properties = new DeclensionMatchProperties();
        properties.setPairsPerItem(5);

        generator = new DeclensionQuizItemGenerator(
                lexemeRepository, questItemRepository, properties, objectMapper);
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

    @Test
    void supportedDomain_returnsGrammar() {
        assertThat(generator.isDomainSupported(TopicDomain.NOMINAL_MORPHOLOGY)).isTrue();
    }

    @Test
    void generate_aStemTopic_createsAllFourTypes() {
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection()))
                .thenReturn(List.of(lexeme("nara", "नर", LexemeGender.MASCULINE, "a-stem-masc")));
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
    void generate_aStemMasculine_composesCanonicalForms() throws Exception {
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection()))
                .thenReturn(List.of(lexeme("nara", "नर", LexemeGender.MASCULINE, "a-stem-masc")));
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
        assertThat(payloadsByCell.get("NOMINATIVE:PLURAL").correctFormIast()).isEqualTo("narāḥ");
        assertThat(payloadsByCell.get("INSTRUMENTAL:PLURAL").correctFormIast()).isEqualTo("naraiḥ");
        assertThat(payloadsByCell.get("LOCATIVE:PLURAL").correctFormIast()).isEqualTo("nareṣu");
        assertThat(payloadsByCell.get("DATIVE:PLURAL").correctFormIast()).isEqualTo("narebhyaḥ");

        assertThat(payloadsByCell.get("NOMINATIVE:SINGULAR").correctFormDevanagari()).isEqualTo("नरः");
        assertThat(payloadsByCell.get("INSTRUMENTAL:SINGULAR").correctFormDevanagari()).isEqualTo("नरेन");
    }

    @Test
    void generate_setsProgressTagOnAllItems() throws Exception {
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection()))
                .thenReturn(List.of(lexeme("nara", "नर", LexemeGender.MASCULINE, "a-stem-masc")));
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
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection())).thenReturn(List.of(
                lexeme("agni", "अग्नि", LexemeGender.MASCULINE, "i-stem"),
                lexeme("agni", "अग्नि", LexemeGender.NEUTER, "i-stem")));
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
        when(lexemeRepository.findWithMorphologyByCodeIn(anyCollection())).thenReturn(many);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<QuestItem> items = invocation.getArgument(0);
            items.forEach(item -> item.setId(UUID.randomUUID()));
            return items;
        });

        // 10 lexemes * (24 + 24 + 24 + 5) items each
        assertThat(generator.generate(topic("a-stem"))).isEqualTo(10 * 77);
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