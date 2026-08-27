package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LemmaTranslation;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaLexicalTopicRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LemmaTranslationRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticClassRepository;
import sm.selflearn.samskrtam.common.transliteration.TransliterationService;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.model.TopicDomainType;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.AnswerMode;
import sm.selflearn.samskrtam.quest.lexicon.VocabularyWordPayload;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LexicalQuizItemGeneratorTest {

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SEMANTIC_CLASS_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private TopicRepository topicRepository;
    private QuestItemRepository questItemRepository;
    private SemanticClassRepository semanticClassRepository;
    private FrequencyBandRepository frequencyBandRepository;
    private PartOfSpeechRepository partOfSpeechRepository;
    private LemmaTranslationRepository lemmaTranslationRepository;
    private LemmaLexicalTopicRepository lemmaLexicalTopicRepository;
    private TransliterationService transliterationService;
    private ObjectMapper objectMapper;
    private LexicalQuizItemGenerator generator;

    @BeforeEach
    void setUp() {
        topicRepository = mock(TopicRepository.class);
        questItemRepository = mock(QuestItemRepository.class);
        semanticClassRepository = mock(SemanticClassRepository.class);
        frequencyBandRepository = mock(FrequencyBandRepository.class);
        partOfSpeechRepository = mock(PartOfSpeechRepository.class);
        lemmaTranslationRepository = mock(LemmaTranslationRepository.class);
        lemmaLexicalTopicRepository = mock(LemmaLexicalTopicRepository.class);
        transliterationService = mock(TransliterationService.class);
        objectMapper = new ObjectMapper();

        generator = new LexicalQuizItemGenerator(
                topicRepository, questItemRepository, semanticClassRepository,
                frequencyBandRepository, partOfSpeechRepository,
                lemmaTranslationRepository, lemmaLexicalTopicRepository,
                transliterationService, objectMapper);

        // devanagari = iast surrounded by parentheses-friendly marker for assertions
        when(transliterationService.iastToDevanagari(any()))
                .thenAnswer(inv -> "{" + inv.getArgument(0) + "}");
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

    private Topic topicWithId(UUID id, String code) {
        Topic topic = new Topic();
        topic.setId(id);
        topic.setCode(code);
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

    private LemmaTranslation tr(String lemma, String lang, String gloss, boolean main) {
        LemmaTranslation t = new LemmaTranslation();
        t.setLemmaIast(lemma);
        t.setLanguage(lang);
        t.setGloss(gloss);
        t.setMain(main);
        return t;
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

    @Test
    void supportedDomain_includesLexiconAndVerse() {
        assertThat(generator.isDomainSupported(TopicDomain.LEXICON)).isTrue();
        assertThat(generator.isDomainSupported(TopicDomain.VERSE)).isTrue();
    }

    @Test
    void ensureTopicsExist_createsMissingSemanticTopic() {
        SemanticClass root = semanticClass("nature", null);
        SemanticClass animals = semanticClass("animals", root);
        when(semanticClassRepository.findAll()).thenReturn(List.of(root, animals));
        when(topicRepository.findByCode("animals")).thenReturn(Optional.empty());
        when(topicRepository.findByCode("nature")).thenReturn(Optional.of(topic("nature")));
        when(topicRepository.findByCode("lex-frequency-top500"))
                .thenReturn(Optional.of(topicWithId(UUID.randomUUID(), "lex-frequency-top500")));
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        generator.ensureTopicsExist();

        var captor = org.mockito.ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(captor.capture());
        Topic created = captor.getValue();
        assertThat(created.getCode()).isEqualTo("animals");
        assertThat(created.getDomain()).isEqualTo(TopicDomain.LEXICON);
        assertThat(created.getSemanticClasses())
                .extracting(SemanticClass::getCode)
                .containsExactly("animals");
    }

    @Test
    void ensureTopicsExist_createsFrequencyAndPosTopics() {
        when(frequencyBandRepository.findAllByOrderBySortOrderAsc())
                .thenReturn(List.of(band("CORE", 1, 100, 1), band("ESSENTIAL", 101, 250, 2)));
        when(topicRepository.findByCode("lex-frequency-core"))
                .thenReturn(Optional.of(topicWithId(UUID.randomUUID(), "lex-frequency-core")));
        when(topicRepository.findByCode("lex-frequency-essential")).thenReturn(Optional.empty());
        when(topicRepository.findByCode("lex-frequency-top500")).thenReturn(Optional.empty());

        PartOfSpeech noun = new PartOfSpeech();
        noun.setCode("noun");
        noun.setNameRu("Существительное");
        noun.setNameEn("Noun");
        when(partOfSpeechRepository.findAll()).thenReturn(List.of(noun));
        when(topicRepository.findByCode("lex-pos-noun")).thenReturn(Optional.empty());
        when(topicRepository.save(any(Topic.class))).thenAnswer(i -> i.getArgument(0));

        generator.ensureTopicsExist();

        var captor = org.mockito.ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository, org.mockito.Mockito.atLeast(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Topic::getCode)
                .contains("lex-frequency-essential", "lex-frequency-top500", "lex-pos-noun");
    }

    @Test
    void generate_semanticTopic_createsVocabItems() throws Exception {
        stubLemmaIastsBySemanticClass(SEMANTIC_CLASS_ID,
                List.of("nara", "asva", "gaja", "simha", "vyaghra"));
        stubTranslations(List.of(
                tr("nara", "en", "man", true), tr("nara", "ru", "muzhchina", true),
                tr("asva", "en", "horse", true), tr("asva", "ru", "loshad", true),
                tr("gaja", "en", "elephant", true), tr("gaja", "ru", "slon", true),
                tr("simha", "en", "lion", true), tr("simha", "ru", "lev", true),
                tr("vyaghra", "en", "tiger", true), tr("vyaghra", "ru", "tigr", true)));
        when(questItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        assertThat(generator.generate(topic("animals"))).isEqualTo(5);

        List<QuestItem> saved = lastSavedItems();
        assertThat(saved).hasSize(5);
        assertThat(saved).allMatch(item -> item.getItemType().equals("VOCABULARY_WORD"));
        assertThat(saved).allMatch(item -> item.getAnswerMode() == AnswerMode.SINGLE_CHOICE);
        assertThat(saved).allMatch(item -> item.getGeneratorSource().equals("LEXICAL_BATCH"));
        assertThat(saved).allMatch(item -> item.getPrompt().startsWith("What does "));

        QuestItem man = saved.stream()
                .filter(item -> item.getProgressTag().equals("nara"))
                .findFirst().orElseThrow();
        assertThat(man.getCorrectAnswer()).isEqualTo("man");
        assertThat(man.getCorrectAnswerRu()).isEqualTo("muzhchina");
        assertThat(man.getDistractorsRu()).contains("loshad", "lev", "tigr");

        VocabularyWordPayload payload = read(man.getPayload(), VocabularyWordPayload.class);
        assertThat(payload.lemmaSlp1()).isNull();
        assertThat(payload.lemmaIast()).isEqualTo("nara");
        assertThat(payload.lemmaDevanagari()).isEqualTo("{nara}");
        assertThat(payload.glossEn()).isEqualTo("man");
        assertThat(payload.glossRu()).isEqualTo("muzhchina");
        assertThat(man.getPrompt()).isEqualTo("What does nara ({nara}) mean?");
        assertThat(man.getPromptRu()).isEqualTo("Что значит nara ({nara})?");
        assertThat(payload.highlights()).extracting(h -> h.text()).containsExactly("nara");
    }

    @Test
    void generate_distractorsExcludeCorrectGloss() throws Exception {
        stubLemmaIastsBySemanticClass(SEMANTIC_CLASS_ID,
                List.of("nara", "naraka", "gaja", "simha", "vyaghra"));
        stubTranslations(List.of(
                tr("nara", "en", "man", true), tr("nara", "ru", "muzhchina", true),
                tr("naraka", "en", "man", true), tr("naraka", "ru", "muzhchina", true),
                tr("gaja", "en", "elephant", true), tr("gaja", "ru", "slon", true),
                tr("simha", "en", "lion", true), tr("simha", "ru", "lev", true),
                tr("vyaghra", "en", "tiger", true), tr("vyaghra", "ru", "tigr", true)));
        when(questItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        generator.generate(topic("animals"));

        QuestItem man = lastSavedItems().stream()
                .filter(item -> item.getProgressTag().equals("nara"))
                .findFirst().orElseThrow();
        assertThat(man.getDistractors()).doesNotContain("man");
        assertThat(man.getDistractorsRu()).doesNotContain("muzhchina");
    }

    @Test
    void generate_insufficientTranslations_noop() {
        stubLemmaIastsBySemanticClass(SEMANTIC_CLASS_ID, List.of("nara", "gaja", "simha"));
        stubTranslations(List.of(
                tr("nara", "en", "man", true), tr("nara", "ru", "muzhchina", true),
                tr("gaja", "en", "elephant", true), tr("gaja", "ru", "slon", true),
                tr("simha", "en", "lion", true), tr("simha", "ru", "lev", true)));

        assertThat(generator.generate(topic("animals"))).isZero();
        verify(questItemRepository, never()).saveAll(anyList());
    }

    @Test
    void generate_skipsLemmaMissingOneLanguage() throws Exception {
        // "ksana" has only en -> dropped, so 4 usable lemmas remain -> 4 items
        stubLemmaIastsBySemanticClass(SEMANTIC_CLASS_ID,
                List.of("ksana", "nara", "gaja", "simha", "vyaghra"));
        stubTranslations(List.of(
                tr("ksana", "en", "moment", true),
                tr("nara", "en", "man", true), tr("nara", "ru", "muzhchina", true),
                tr("gaja", "en", "elephant", true), tr("gaja", "ru", "slon", true),
                tr("simha", "en", "lion", true), tr("simha", "ru", "lev", true),
                tr("vyaghra", "en", "tiger", true), tr("vyaghra", "ru", "tigr", true)));
        when(questItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        assertThat(generator.generate(topic("animals"))).isEqualTo(4);
        assertThat(lastSavedItems().stream().map(QuestItem::getProgressTag)).doesNotContain("ksana");
    }

    @Test
    void generate_frequencyTopic_usesRankWindow() {
        UUID bandId = UUID.randomUUID();
        when(frequencyBandRepository.findByCode("CORE")).thenReturn(Optional.of(band("CORE", 1, 100, 1)));
        Topic freqTopic = topicWithId(bandId, "lex-frequency-core");
        when(lemmaTranslationRepository.findDistinctLemmaIastByFrequencyRankRange(1, 100))
                .thenReturn(List.of("nara", "asva", "gaja", "simha"));
        stubTranslations(List.of(
                tr("nara", "en", "man", true), tr("nara", "ru", "muzhchina", true),
                tr("asva", "en", "horse", true), tr("asva", "ru", "loshad", true),
                tr("gaja", "en", "elephant", true), tr("gaja", "ru", "slon", true),
                tr("simha", "en", "lion", true), tr("simha", "ru", "lev", true)));
        when(questItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        assertThat(generator.generate(freqTopic)).isEqualTo(4);
    }

    @Test
    void generate_posTopic_matchesFrischPos() {
        UUID nounId = UUID.randomUUID();
        Topic posTopic = topicWithId(nounId, "lex-pos-noun");
        when(lemmaTranslationRepository.findDistinctLemmaIastByPos("NOUN"))
                .thenReturn(List.of("nara", "asva", "gaja", "simha"));
        stubTranslations(List.of(
                tr("nara", "en", "man", true), tr("nara", "ru", "muzhchina", true),
                tr("asva", "en", "horse", true), tr("asva", "ru", "loshad", true),
                tr("gaja", "en", "elephant", true), tr("gaja", "ru", "slon", true),
                tr("simha", "en", "lion", true), tr("simha", "ru", "lev", true)));
        when(questItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        assertThat(generator.generate(posTopic)).isEqualTo(4);
    }

    @Test
    void generate_verseTopic_resolvesFromLemmaLexicalTopic() {
        UUID topicId = UUID.randomUUID();
        Topic verseTopic = new Topic();
        verseTopic.setId(topicId);
        verseTopic.setCode("user-abc");
        verseTopic.setDomain(TopicDomain.VERSE);
        when(lemmaLexicalTopicRepository.findDistinctLemmaIastByTopicCode("user-abc"))
                .thenReturn(List.of("nara", "asva", "gaja", "simha"));
        stubTranslations(List.of(
                tr("nara", "en", "man", true), tr("nara", "ru", "muzhchina", true),
                tr("asva", "en", "horse", true), tr("asva", "ru", "loshad", true),
                tr("gaja", "en", "elephant", true), tr("gaja", "ru", "slon", true),
                tr("simha", "en", "lion", true), tr("simha", "ru", "lev", true)));
        when(questItemRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        assertThat(generator.generate(verseTopic)).isEqualTo(4);
    }

    @Test
    void generate_noLemmaIasts_noop() {
        stubLemmaIastsBySemanticClass(SEMANTIC_CLASS_ID, List.of());

        assertThat(generator.generate(topic("animals"))).isZero();
        verify(questItemRepository, never()).saveAll(anyList());
    }

    // ----------------------------------------------------------------------
    // Stubs
    // ----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private void stubLemmaIastsBySemanticClass(UUID classId, List<String> lemmas) {
        when(lemmaTranslationRepository.findDistinctLemmaIastBySemanticClassIds(anySet()))
                .thenAnswer(invocation -> {
                    Set<UUID> requested = invocation.getArgument(0);
                    return requested.contains(classId) ? lemmas : List.of();
                });
    }

    @SuppressWarnings("unchecked")
    private void stubTranslations(List<LemmaTranslation> rows) {
        when(lemmaTranslationRepository.findByLemmaIastIn(anySet()))
                .thenAnswer(invocation -> {
                    Set<String> requested = invocation.getArgument(0);
                    return rows.stream()
                            .filter(r -> requested.contains(r.getLemmaIast()))
                            .toList();
                });
    }
}
