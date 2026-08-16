package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.LexiconImportService;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeLexicalTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeLexicalTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticClassRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.lexicon.VocabularyWordPayload;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LexicalQuizItemGeneratorTest {

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SEMANTIC_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private TopicRepository topicRepository;
    private LexemeRepository lexemeRepository;
    private QuestItemRepository questItemRepository;
    private SemanticClassRepository semanticClassRepository;
    private LexemeLexicalTopicRepository lexemeLexicalTopicRepository;
    private FrequencyBandRepository frequencyBandRepository;
    private LexemeFrequencyRepository lexemeFrequencyRepository;
    private PartOfSpeechRepository partOfSpeechRepository;
    private ObjectMapper objectMapper;
    private LexicalQuizItemGenerator generator;

    @BeforeEach
    void setUp() {
        topicRepository = mock(TopicRepository.class);
        lexemeRepository = mock(LexemeRepository.class);
        questItemRepository = mock(QuestItemRepository.class);
        semanticClassRepository = mock(SemanticClassRepository.class);
        lexemeLexicalTopicRepository = mock(LexemeLexicalTopicRepository.class);
        frequencyBandRepository = mock(FrequencyBandRepository.class);
        lexemeFrequencyRepository = mock(LexemeFrequencyRepository.class);
        partOfSpeechRepository = mock(PartOfSpeechRepository.class);
        objectMapper = new ObjectMapper();

        generator = new LexicalQuizItemGenerator(
                topicRepository, lexemeRepository, questItemRepository,
                semanticClassRepository, lexemeLexicalTopicRepository,
                frequencyBandRepository, lexemeFrequencyRepository,
                partOfSpeechRepository, objectMapper);
    }

    private Topic topic(String code) {
        Topic topic = new Topic();
        topic.setId(TOPIC_ID);
        topic.setCode(code);
        SemanticClass sc = new SemanticClass();
        sc.setId(SEMANTIC_CLASS_ID);
        sc.setCode(code);
        topic.setSemanticClasses(Set.of(sc));
        return topic;
    }

    private SemanticClass semanticClass(String code, SemanticClass parent) {
        SemanticClass sc = new SemanticClass();
        sc.setId(UUID.randomUUID());
        sc.setCode(code);
        sc.setNameRu("ru");
        sc.setNameEn("en");
        sc.setParent(parent);
        return sc;
    }

    private Lexeme lexeme(String slp1, String iast, String deva, String glossEn, String glossRu) {
        Lexeme lexeme = new Lexeme();
        lexeme.setId(UUID.randomUUID());
        lexeme.setLemmaSlp1(slp1);
        lexeme.setLemmaIast(iast);
        lexeme.setLemmaDevanagari(deva);
        lexeme.setGlossEn(glossEn);
        lexeme.setGlossRu(glossRu);
        
        return lexeme;
    }

    private void stubLexemes(List<Lexeme> lexemes) {
        List<UUID> ids = lexemes.stream().map(Lexeme::getId).toList();
        when(lexemeRepository.findLexemeIdsBySemanticClassIds(anyCollection())).thenReturn(ids);
        when(lexemeRepository.findWithDetailsByIdIn(anyCollection())).thenReturn(lexemes);
    }

    @Test
    void supportedDomain_returnsLexicon() {
        assertThat(generator.isDomainSupported(TopicDomain.LEXICON)).isTrue();
    }

    @Test
    void ensureTopicsExist_createsMissingTopics() {
        SemanticClass root = semanticClass("nature", null);
        SemanticClass animals = semanticClass("animals", root);
        when(semanticClassRepository.findAll()).thenReturn(List.of(root, animals));
        when(topicRepository.findByCode("animals")).thenReturn(java.util.Optional.empty());
        when(topicRepository.findByCode("nature")).thenReturn(java.util.Optional.of(topic("nature")));
        // frequency lessons already exist (regeneration path), see separate test
        when(topicRepository.findByCode("lex-frequency-top500")).thenReturn(java.util.Optional.of(topic("lex-frequency-top500")));
        when(lexemeFrequencyRepository.findLexemeIdsBySourceAndRankRange(
                LexiconImportService.FREQUENCY_SOURCE, 1, 500)).thenReturn(List.of());

        generator.ensureTopicsExist();

        org.mockito.ArgumentCaptor<Topic> captor = org.mockito.ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(captor.capture());
        Topic created = captor.getValue();
        assertThat(created.getCode()).isEqualTo("animals");
        assertThat(created.getDomain()).isEqualTo(TopicDomain.LEXICON);
        assertThat(created.getSemanticClasses())
                .extracting(SemanticClass::getCode)
                .containsExactly("animals");
    }

    @SuppressWarnings("unchecked")
    @Test
    void ensureTopicsExist_rebindsFrequencyLessons_fromBands() {
        UUID coreId = UUID.randomUUID();
        UUID essentialId = UUID.randomUUID();
        UUID top500Id = UUID.randomUUID();

        when(frequencyBandRepository.findAllByOrderBySortOrderAsc()).thenReturn(List.of(
                band("CORE", 1, 100, 1),
                band("ESSENTIAL", 101, 250, 2)));
        when(topicRepository.findByCode("lex-frequency-core"))
                .thenReturn(Optional.of(topicWithId(coreId, "lex-frequency-core")));
        when(topicRepository.findByCode("lex-frequency-essential")).thenReturn(Optional.empty());
        when(topicRepository.findByCode("lex-frequency-top500")).thenReturn(Optional.empty());
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic t = invocation.getArgument(0);
            t.setId("lex-frequency-top500".equals(t.getCode()) ? top500Id : essentialId);
            return t;
        });

        UUID w1 = UUID.randomUUID();
        UUID w2 = UUID.randomUUID();
        UUID w3 = UUID.randomUUID();
        UUID w4 = UUID.randomUUID();
        when(lexemeFrequencyRepository.findLexemeIdsBySourceAndRankRange(
                LexiconImportService.FREQUENCY_SOURCE, 1, 100)).thenReturn(List.of(w1, w2));
        when(lexemeFrequencyRepository.findLexemeIdsBySourceAndRankRange(
                LexiconImportService.FREQUENCY_SOURCE, 101, 250)).thenReturn(List.of(w3));
        when(lexemeFrequencyRepository.findLexemeIdsBySourceAndRankRange(
                LexiconImportService.FREQUENCY_SOURCE, 1, 500)).thenReturn(List.of(w4));

        generator.ensureTopicsExist();

        verify(lexemeLexicalTopicRepository).deleteByIdLexicalTopicId(coreId);
        verify(lexemeLexicalTopicRepository).deleteByIdLexicalTopicId(essentialId);
        verify(lexemeLexicalTopicRepository).deleteByIdLexicalTopicId(top500Id);

        org.mockito.ArgumentCaptor<List<LexemeLexicalTopic>> bindingsCaptor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(lexemeLexicalTopicRepository, times(3)).saveAll(bindingsCaptor.capture());
        List<LexemeLexicalTopic> all = bindingsCaptor.getAllValues().stream()
                .flatMap(List::stream).toList();
        assertThat(all).hasSize(4);

        assertThat(all).filteredOn(b -> b.getId().getLexicalTopicId().equals(coreId))
                .extracting(b -> b.getId().getLexemeId())
                .containsExactlyInAnyOrder(w1, w2);
        assertThat(all).filteredOn(b -> b.getId().getLexicalTopicId().equals(essentialId))
                .extracting(b -> b.getId().getLexemeId())
                .containsExactly(w3);
        assertThat(all).filteredOn(b -> b.getId().getLexicalTopicId().equals(top500Id))
                .extracting(b -> b.getId().getLexemeId())
                .containsExactly(w4);

        org.mockito.ArgumentCaptor<Topic> topicCaptor = org.mockito.ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository, times(2)).save(topicCaptor.capture());
        Topic essential = topicCaptor.getAllValues().stream()
                .filter(t -> t.getCode().equals("lex-frequency-essential"))
                .findFirst().orElseThrow();
        assertThat(essential.getDomain()).isEqualTo(TopicDomain.LEXICON);
        assertThat(essential.getDomainType()).isEqualTo(TopicDomainType.LEXICON);

        Topic top500 = topicCaptor.getAllValues().stream()
                .filter(t -> t.getCode().equals("lex-frequency-top500"))
                .findFirst().orElseThrow();
        assertThat(top500.getDomain()).isEqualTo(TopicDomain.LEXICON);
        assertThat(top500.getDomainType()).isEqualTo(TopicDomainType.LEXICON);
        assertThat(top500.isEvergreen()).isTrue();
        assertThat(top500.getLearningLevel()).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void ensureTopicsExist_rebindsPosLessons() {
        UUID nounId = UUID.randomUUID();

        PartOfSpeech noun = new PartOfSpeech();
        noun.setCode("noun");
        noun.setNameRu("Существительное");
        noun.setNameEn("Noun");
        when(partOfSpeechRepository.findAll()).thenReturn(List.of(noun));
        when(topicRepository.findByCode("lex-pos-noun")).thenReturn(Optional.empty());
        // frequency/top500 already exist (tested separately), stub to avoid side effects
        when(topicRepository.findByCode("lex-frequency-top500"))
                .thenReturn(Optional.of(topicWithId(UUID.randomUUID(), "lex-frequency-top500")));
        when(lexemeFrequencyRepository.findLexemeIdsBySourceAndRankRange(
                LexiconImportService.FREQUENCY_SOURCE, 1, 500)).thenReturn(List.of());
        when(topicRepository.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic t = invocation.getArgument(0);
            t.setId(nounId);
            return t;
        });

        Lexeme w1 = lexeme("nara", "nara", "नर", "man", "мужчина");
        Lexeme w2 = lexeme("aśva", "asva", "अश्व", "horse", "лошадь");
        when(lexemeRepository.findByPartsOfSpeech_CodeIn(Set.of("noun")))
                .thenReturn(List.of(w1, w2));

        generator.ensureTopicsExist();

        verify(lexemeLexicalTopicRepository).deleteByIdLexicalTopicId(nounId);

        org.mockito.ArgumentCaptor<List<LexemeLexicalTopic>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        // two saveAll calls: empty from rebindTop500Lesson + populated from rebindPosLessons
        verify(lexemeLexicalTopicRepository, times(2)).saveAll(captor.capture());
        List<LexemeLexicalTopic> posBindings = captor.getAllValues().stream()
                .filter(b -> !b.isEmpty())
                .findFirst().orElseThrow();
        assertThat(posBindings)
                .extracting(b -> b.getId().getLexemeId())
                .containsExactlyInAnyOrder(w1.getId(), w2.getId());

        org.mockito.ArgumentCaptor<Topic> topicCaptor = org.mockito.ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(topicCaptor.capture());
        Topic created = topicCaptor.getValue();
        assertThat(created.getCode()).isEqualTo("lex-pos-noun");
        assertThat(created.getDomain()).isEqualTo(TopicDomain.LEXICON);
        assertThat(created.getDomainType()).isEqualTo(TopicDomainType.LEXICON);
        assertThat(created.isEvergreen()).isTrue();
    }

    private FrequencyBand band(String code, int minRank, int maxRank, int sortOrder) {
        FrequencyBand band = new FrequencyBand();
        band.setCode(code);
        band.setMinRank(minRank);
        band.setMaxRank(maxRank);
        band.setLabelRu("label");
        band.setLabelEn("label");
        band.setSortOrder((short) sortOrder);
        return band;
    }

    private Topic topicWithId(UUID id, String code) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setCode(code);
        return topic;
    }

    @Test
    void generate_lexicalTopic_createsVocabItems() throws Exception {
        List<Lexeme> lexemes = List.of(
                lexeme("nara", "nara", "नर", "man", "мужчина"),
                lexeme("aśva", "asva", "अश्व", "horse", "лошадь"),
                lexeme("gaja", "gaja", "गज", "elephant", "слон"),
                lexeme("siṃha", "simha", "सिंह", "lion", "лев"),
                lexeme("vyāghra", "vyaghra", "व्याघ्र", "tiger", "тигр"));
        stubLexemes(lexemes);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(generator.generate(topic("lex-animals"))).isEqualTo(5);

        List<QuestItem> saved = lastSavedItems();
        assertThat(saved).hasSize(5);
        assertThat(saved).allMatch(item -> item.getItemType().equals("VOCABULARY_WORD"));
        assertThat(saved).allMatch(item -> item.getAnswerMode().equals("SINGLE_CHOICE"));
        assertThat(saved).allMatch(item -> item.getGeneratorSource().equals("LEXICAL_BATCH"));
        assertThat(saved).allMatch(item -> item.getPrompt().startsWith("What does '"));

        QuestItem man = saved.stream()
                .filter(item -> item.getProgressTag().equals("nara"))
                .findFirst().orElseThrow();
        assertThat(man.getCorrectAnswer()).isEqualTo("man");
        assertThat(man.getCorrectAnswerRu()).isEqualTo("мужчина");
        assertThat(man.getDistractorsRu()).contains("лошадь", "слон", "лев");

        VocabularyWordPayload payload = read(man.getPayload(), VocabularyWordPayload.class);
        assertThat(payload.lemmaSlp1()).isEqualTo("nara");
        assertThat(payload.lemmaIast()).isEqualTo("nara");
        assertThat(payload.lemmaDevanagari()).isEqualTo("नर");
        assertThat(payload.glossEn()).isEqualTo("man");
        assertThat(payload.glossRu()).isEqualTo("мужчина");
    }

    @Test
    void generate_distractorsExcludeCorrectGloss() throws Exception {
        List<Lexeme> lexemes = List.of(
                lexeme("nara", "nara", "नर", "man", "мужчина"),
                lexeme("naraka", "naraka", "नरक", "man", "мужчина"),
                lexeme("gaja", "gaja", "गज", "elephant", "слон"),
                lexeme("siṃha", "simha", "सिंह", "lion", "лев"),
                lexeme("vyāghra", "vyaghra", "व्याघ्र", "tiger", "тигр"));
        stubLexemes(lexemes);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        generator.generate(topic("lex-animals"));

        QuestItem man = lastSavedItems().stream()
                .filter(item -> item.getProgressTag().equals("nara"))
                .findFirst().orElseThrow();
        assertThat(man.getDistractors()).doesNotContain("man");
        assertThat(man.getDistractorsRu()).doesNotContain("мужчина");
    }

    @Test
    void generate_insufficientGlossedLexemes_noop() {
        List<Lexeme> lexemes = List.of(
                lexeme("nara", "nara", "नर", "man", "мужчина"),
                lexeme("gaja", "gaja", "गज", "elephant", "слон"),
                lexeme("siṃha", "simha", "सिंह", "lion", "лев"));
        stubLexemes(lexemes);

        assertThat(generator.generate(topic("lex-animals"))).isZero();
        verify(questItemRepository, never()).saveAll(anyList());
    }

    @Test
    void generate_unglossedLexemes_skipped() {
        Lexeme unglossed = new Lexeme();
        unglossed.setId(UUID.randomUUID());
        unglossed.setLemmaSlp1("kṣaṇa");

        List<Lexeme> lexemes = List.of(
                unglossed,
                lexeme("nara", "nara", "नर", "man", "мужчина"),
                lexeme("gaja", "gaja", "गज", "elephant", "слон"),
                lexeme("siṃha", "simha", "सिंह", "lion", "лев"),
                lexeme("vyāghra", "vyaghra", "व्याघ्र", "tiger", "тигр"));
        stubLexemes(lexemes);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(generator.generate(topic("lex-animals"))).isEqualTo(4);
        assertThat(lastSavedItems().stream().map(QuestItem::getProgressTag))
                .doesNotContain("kṣaṇa");
    }

    @Test
    void generate_noLexemes_noop() {
        stubLexemes(List.of());

        assertThat(generator.generate(topic("lex-animals"))).isZero();
        verify(questItemRepository, never()).saveAll(anyList());
    }

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