package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemGenerationKeyRepository;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.declension.CaseRecognitionPayload;
import sm.selflearn.samskrtam.quest.declension.DeclensionFormPayload;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeclensionQuestItemBatchGeneratorTest {

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TopicRepository topicRepository;
    private LexemeRepository lexemeRepository;
    private QuestItemRepository questItemRepository;
    private QuestItemGenerationKeyRepository generationKeyRepository;
    private ObjectMapper objectMapper;
    private DeclensionQuestItemBatchGenerator generator;

    @BeforeEach
    void setUp() {
        topicRepository = mock(TopicRepository.class);
        lexemeRepository = mock(LexemeRepository.class);
        questItemRepository = mock(QuestItemRepository.class);
        generationKeyRepository = mock(QuestItemGenerationKeyRepository.class);
        objectMapper = new ObjectMapper();

        DeclensionMatchProperties properties = new DeclensionMatchProperties();
        properties.setPairsPerItem(5);

        generator = new DeclensionQuestItemBatchGenerator(
                topicRepository, lexemeRepository, questItemRepository,
                generationKeyRepository, properties, objectMapper);
    }

    private Topic topic(String code) {
        Topic topic = new Topic();
        topic.setId(TOPIC_ID);
        topic.setCode(code);
        return topic;
    }

    private Lexeme lexeme(String lemmaIast, String lemmaDevanagari, LexemeGender gender) {
        Lexeme lexeme = new Lexeme();
        lexeme.setId(UUID.randomUUID());
        lexeme.setLemmaIast(lemmaIast);
        lexeme.setLemmaDevanagari(lemmaDevanagari);
        lexeme.setGender(gender);
        return lexeme;
    }

    // ------------------------------------------------------------------
    // Idempotency (DoD: re-run must not duplicate items)
    // ------------------------------------------------------------------

    @Test
    void generateForTopic_existingGenerationKey_createsNothing() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic("a-stem-masc")));
        when(lexemeRepository.findByMorphologyClasses_Code("a-stem-masc"))
                .thenReturn(List.of(lexeme("nara", "नर", LexemeGender.MASCULINE)));
        when(generationKeyRepository.existsByGenerationKey(anyString())).thenReturn(true);

        assertThat(generator.generateForTopic(TOPIC_ID, 0)).isZero();
        verify(questItemRepository, never()).saveAll(anyList());
        verify(generationKeyRepository, never()).saveAll(anyList());
    }

    @Test
    void generateForTopic_newTopic_createsAllFourTypesAndKeys() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic("a-stem-masc")));
        when(lexemeRepository.findByMorphologyClasses_Code("a-stem-masc"))
                .thenReturn(List.of(lexeme("nara", "नर", LexemeGender.MASCULINE)));
        when(generationKeyRepository.existsByGenerationKey(anyString())).thenReturn(false);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<QuestItem> items = invocation.getArgument(0);
            items.forEach(item -> item.setId(UUID.randomUUID()));
            return items;
        });

        // 24 cells -> 24 FORM + 24 FORM_CHOICE + 24 CASE_RECOGNITION + 5 MATCH blocks (ceil(24/5))
        assertThat(generator.generateForTopic(TOPIC_ID, 0)).isEqualTo(77);

        verify(questItemRepository).saveAll(anyList());
        verify(generationKeyRepository).saveAll(org.mockito.ArgumentMatchers.<List>argThat(keys -> keys.size() == 77));
    }

    @Test
    void generateForTopic_unknownTopic_throwsEntityNotFound() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> generator.generateForTopic(TOPIC_ID, 0))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void generateForTopic_nonDeclensionTopic_noop() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic("class-1")));

        assertThat(generator.generateForTopic(TOPIC_ID, 0)).isZero();
        verify(questItemRepository, never()).saveAll(anyList());
    }

    @Test
    void generateForTopic_noLexemes_noop() {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic("a-stem-masc")));
        when(lexemeRepository.findByMorphologyClasses_Code("a-stem-masc")).thenReturn(List.of());

        assertThat(generator.generateForTopic(TOPIC_ID, 0)).isZero();
        verify(questItemRepository, never()).saveAll(anyList());
    }

    // ------------------------------------------------------------------
    // Word form composition (paradigm port sanity checks)
    // ------------------------------------------------------------------

    @Test
    void generateForTopic_aStemMasculine_composesCanonicalForms() throws Exception {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic("a-stem-masc")));
        when(lexemeRepository.findByMorphologyClasses_Code("a-stem-masc"))
                .thenReturn(List.of(lexeme("nara", "नर", LexemeGender.MASCULINE)));
        when(generationKeyRepository.existsByGenerationKey(anyString())).thenReturn(false);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        generator.generateForTopic(TOPIC_ID, 0);

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

    // ------------------------------------------------------------------
    // genderRequired (DoD: form ambiguous across genders -> gender needed)
    // ------------------------------------------------------------------

    @Test
    void generateForTopic_ambiguousFormAcrossGenders_genderRequiredTrue() throws Exception {
        when(topicRepository.findById(TOPIC_ID)).thenReturn(java.util.Optional.of(topic("i-stem")));
        when(lexemeRepository.findByMorphologyClasses_Code("i-stem")).thenReturn(List.of(
                lexeme("agni", "अग्नि", LexemeGender.MASCULINE),
                lexeme("agni", "अग्नि", LexemeGender.NEUTER)));
        when(generationKeyRepository.existsByGenerationKey(anyString())).thenReturn(false);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        generator.generateForTopic(TOPIC_ID, 0);

        List<CaseRecognitionPayload> caseItems = lastSavedItems().stream()
                .filter(item -> item.getItemType().equals("CASE_RECOGNITION"))
                .map(item -> read(item.getPayload(), CaseRecognitionPayload.class))
                .toList();

        // instrumental singular "agninā" is produced by both masculine and neuter i-stems
        List<CaseRecognitionPayload> instrumental = caseItems.stream()
                .filter(p -> p.correctCaseType().equals("INSTRUMENTAL"))
                .filter(p -> p.correctNumberType().equals("SINGULAR"))
                .toList();
        assertThat(instrumental).hasSize(2);
        assertThat(instrumental).allMatch(p -> p.genderRequired());

        // nominative singular differs between genders ("agniḥ" vs "agni") — gender not needed
        List<CaseRecognitionPayload> nominative = caseItems.stream()
                .filter(p -> p.correctCaseType().equals("NOMINATIVE"))
                .filter(p -> p.correctNumberType().equals("SINGULAR"))
                .toList();
        assertThat(nominative).hasSize(2);
        assertThat(nominative).noneMatch(p -> p.genderRequired());
    }

    @Test
    void requiresGender_mapWithMultipleGenders_returnsTrue() {
        Map<String, Set<LexemeGender>> formGenders = Map.of(
                "agninā", Set.of(LexemeGender.MASCULINE, LexemeGender.NEUTER));

        assertThat(DeclensionQuestItemBatchGenerator.requiresGender(formGenders, "agninā")).isTrue();
    }

    @Test
    void requiresGender_singleGender_returnsFalse() {
        Map<String, Set<LexemeGender>> formGenders = Map.of(
                "agniḥ", Set.of(LexemeGender.MASCULINE));

        assertThat(DeclensionQuestItemBatchGenerator.requiresGender(formGenders, "agniḥ")).isFalse();
    }

    @Test
    void requiresGender_unknownForm_returnsFalse() {
        assertThat(DeclensionQuestItemBatchGenerator.requiresGender(
                Map.of("agniḥ", Set.of(LexemeGender.MASCULINE)), "naraḥ")).isFalse();
        assertThat(DeclensionQuestItemBatchGenerator.requiresGender(null, "naraḥ")).isFalse();
    }

    private List<QuestItem> lastSavedItems() {
        org.mockito.ArgumentCaptor<List> captor = org.mockito.ArgumentCaptor.forClass(List.class);
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
