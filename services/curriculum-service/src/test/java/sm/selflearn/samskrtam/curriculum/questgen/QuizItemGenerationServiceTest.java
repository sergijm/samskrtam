package sm.selflearn.samskrtam.curriculum.questgen;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QuizItemGenerationServiceTest {

    private TopicRepository topicRepository;
    private QuestItemRepository questItemRepository;
    private QuizItemGenerator declensionGenerator;
    private QuizItemGenerator unusedGenerator;

    private Topic aStem;
    private Topic classOne;
    private Topic iUStems;
    private Topic rStems;

    @BeforeEach
    void setUp() {
        topicRepository = mock(TopicRepository.class);
        questItemRepository = mock(QuestItemRepository.class);

        declensionGenerator = mock(QuizItemGenerator.class);
        when(declensionGenerator.supportedTopicSlugs()).thenReturn(Set.of("a-stem", "i-u-stems"));
        unusedGenerator = mock(QuizItemGenerator.class);
        when(unusedGenerator.supportedTopicSlugs()).thenReturn(Set.of());

        aStem = topic("a-stem");
        classOne = topic("class-1");
        iUStems = topic("i-u-stems");
        rStems = topic("r-stems");
    }

    private Topic topic(String code) {
        Topic topic = new Topic();
        topic.setId(UUID.randomUUID());
        topic.setCode(code);
        return topic;
    }

    private QuizItemGenerationService service() {
        return new QuizItemGenerationService(
                List.of(declensionGenerator, unusedGenerator), topicRepository, questItemRepository);
    }

    @Test
    void regenerate_clearsTable_generatesOnlyForSupportedSlugs() {
        when(topicRepository.findAll()).thenReturn(List.of(aStem, classOne, iUStems, rStems));
        when(questItemRepository.deleteAllQuestItems()).thenReturn(99);
        when(declensionGenerator.generate(aStem)).thenReturn(77);
        when(declensionGenerator.generate(iUStems)).thenReturn(152);
        when(questItemRepository.countDistinctProgressTagByTopicId(aStem.getId())).thenReturn(24L);
        when(questItemRepository.countDistinctProgressTagByTopicId(iUStems.getId())).thenReturn(18L);

        Map<String, Map<String, Integer>> stats = service().regenerate();

        verify(questItemRepository).deleteAllQuestItems();
        verify(declensionGenerator).generate(aStem);
        verify(declensionGenerator).generate(iUStems);
        verify(declensionGenerator, never()).generate(classOne);
        verify(declensionGenerator, never()).generate(rStems);
        verify(questItemRepository).countDistinctProgressTagByTopicId(aStem.getId());
        verify(questItemRepository).countDistinctProgressTagByTopicId(iUStems.getId());

        assertThat(stats)
                .containsEntry("a-stem", Map.of("generated", 77, "uniqueProgressTags", 24))
                .containsEntry("i-u-stems", Map.of("generated", 152, "uniqueProgressTags", 18))
                .doesNotContainKeys("class-1", "r-stems");
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