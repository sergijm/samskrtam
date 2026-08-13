package sm.selflearn.samskrtam.curriculum.questgen;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;
import sm.selflearn.samskrtam.quest.lexicon.VocabularyWordPayload;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LexicalQuizItemGeneratorTest {

    private static final UUID TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SEMANTIC_TOPIC_ID = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private TopicRepository topicRepository;
    private LexemeRepository lexemeRepository;
    private QuestItemRepository questItemRepository;
    private ObjectMapper objectMapper;
    private LexicalQuizItemGenerator generator;

    @BeforeEach
    void setUp() {
        topicRepository = mock(TopicRepository.class);
        lexemeRepository = mock(LexemeRepository.class);
        questItemRepository = mock(QuestItemRepository.class);
        objectMapper = new ObjectMapper();

        generator = new LexicalQuizItemGenerator(
                topicRepository, lexemeRepository, questItemRepository, objectMapper);
    }

    private Topic topic(String code) {
        Topic topic = new Topic();
        topic.setId(TOPIC_ID);
        topic.setCode(code);
        topic.setSemanticTopicId(SEMANTIC_TOPIC_ID);
        return topic;
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

    @Test
    void supportedTopicSlugs_returnsLexiconTopics() {
        when(topicRepository.findByDomain(TopicDomain.LEXICON))
                .thenReturn(List.of(topic("lex-animals"), topic("lex-colors")));

        assertThat(generator.supportedTopicSlugs())
                .containsExactlyInAnyOrder("lex-animals", "lex-colors");
    }

    @Test
    void generate_lexicalTopic_createsVocabItems() throws Exception {
        List<Lexeme> lexemes = List.of(
                lexeme("nara", "nara", "नर", "man", "мужчина"),
                lexeme("aśva", "asva", "अश्व", "horse", "лошадь"),
                lexeme("gaja", "gaja", "गज", "elephant", "слон"),
                lexeme("siṃha", "simha", "सिंह", "lion", "лев"),
                lexeme("vyāghra", "vyaghra", "व्याघ्र", "tiger", "тигр"));
        when(lexemeRepository.findBySemanticTopics_Id(SEMANTIC_TOPIC_ID))
                .thenReturn(lexemes);
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
        when(lexemeRepository.findBySemanticTopics_Id(SEMANTIC_TOPIC_ID))
                .thenReturn(lexemes);
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
        when(lexemeRepository.findBySemanticTopics_Id(SEMANTIC_TOPIC_ID))
                .thenReturn(lexemes);

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
        when(lexemeRepository.findBySemanticTopics_Id(SEMANTIC_TOPIC_ID))
                .thenReturn(lexemes);
        when(questItemRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(generator.generate(topic("lex-animals"))).isEqualTo(4);
        assertThat(lastSavedItems().stream().map(QuestItem::getProgressTag))
                .doesNotContain("kṣaṇa");
    }

    @Test
    void generate_noLexemes_noop() {
        when(lexemeRepository.findBySemanticTopics_Id(SEMANTIC_TOPIC_ID))
                .thenReturn(List.of());

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
