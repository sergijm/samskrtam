package sm.selflearn.samskrtam.curriculum.questgen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizItemGenerationServiceTest {

    private ApplicationContext applicationContext;
    private TopicRepository topicRepository;
    private QuestItemRepository questItemRepository;
    private QuizItemGenerator declensionGenerator;
    private QuizItemGenerator unusedGenerator;

    private Topic aStem;
    private Topic classOne;
    private Topic iUStems;

    @BeforeEach
    void setUp() {
        applicationContext = mock(ApplicationContext.class);
        topicRepository = mock(TopicRepository.class);
        questItemRepository = mock(QuestItemRepository.class);

        declensionGenerator = mock(QuizItemGenerator.class);
        when(declensionGenerator.isDomainSupported(TopicDomain.NOMINAL_MORPHOLOGY)).thenReturn(true);
        unusedGenerator = mock(QuizItemGenerator.class);
        when(unusedGenerator.isDomainSupported(TopicDomain.NOMINAL_MORPHOLOGY)).thenReturn(true); // collides, first wins

        when(applicationContext.getBeansOfType(QuizItemGenerator.class))
                .thenReturn(orderedGenerators());

        aStem = topic("a-stem");
        classOne = topic("class-1");
        iUStems = topic("i-u-stems");
    }

    /**
     * Deterministic bean order: Map.of iteration order is unspecified, which made
     * findFirst() (QuizItemGenerationService.regenerate) flaky about which
     * generator handled NOMINAL_MORPHOLOGY topics first.
     */
    private java.util.LinkedHashMap<String, QuizItemGenerator> orderedGenerators() {
        java.util.LinkedHashMap<String, QuizItemGenerator> map = new java.util.LinkedHashMap<>();
        map.put("declensionQuizItemGenerator", declensionGenerator);
        map.put("unusedGenerator", unusedGenerator);
        return map;
    }

    private Topic topic(String code) {
        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setCode(code);
        topic.setDomain(TopicDomain.NOMINAL_MORPHOLOGY);
        return topic;
    }

    private QuizItemGenerationService service() {
        return new QuizItemGenerationService(applicationContext, topicRepository, questItemRepository);
    }

    @Test
    void regenerate_passesAllGrammarTopics_toDeclensionGenerator() {
        when(topicRepository.findAll()).thenReturn(List.of(aStem, classOne, iUStems));
        when(questItemRepository.deleteAllQuestItems()).thenReturn(99);
        when(declensionGenerator.generate(aStem)).thenReturn(77);
        when(declensionGenerator.generate(classOne)).thenReturn(0);
        when(declensionGenerator.generate(iUStems)).thenReturn(152);
        when(questItemRepository.countDistinctProgressTagByTopicId(aStem.getId())).thenReturn(24L);
        when(questItemRepository.countDistinctProgressTagByTopicId(classOne.getId())).thenReturn(0L);
        when(questItemRepository.countDistinctProgressTagByTopicId(iUStems.getId())).thenReturn(18L);

        Map<String, Map<String, Integer>> stats = service().regenerate();

        verify(questItemRepository).deleteAllQuestItems();
        verify(declensionGenerator).generate(aStem);
        verify(declensionGenerator).generate(classOne);
        verify(declensionGenerator).generate(iUStems);

        assertThat(stats)
                .containsEntry("a-stem", Map.of("generated", 77, "uniqueProgressTags", 24))
                .containsEntry("class-1", Map.of("generated", 0, "uniqueProgressTags", 0))
                .containsEntry("i-u-stems", Map.of("generated", 152, "uniqueProgressTags", 18));
    }

    @Test
    void regenerate_noTopics_noop() {
        when(topicRepository.findAll()).thenReturn(List.of());
        when(questItemRepository.deleteAllQuestItems()).thenReturn(0);

        Map<String, Map<String, Integer>> stats = service().regenerate();

        assertThat(stats).isEmpty();
        verify(declensionGenerator, never()).generate(any());
        verify(questItemRepository, never()).countDistinctProgressTagByTopicId(any());
    }
}