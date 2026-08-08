package sm.selflearn.samskrtam.curriculum.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.dto.LearnGraphResponse;
import sm.selflearn.samskrtam.curriculum.dto.LearnLayerDto;
import sm.selflearn.samskrtam.curriculum.dto.LearnTopicDto;
import sm.selflearn.samskrtam.curriculum.mapper.LearnTopicMapper;
import sm.selflearn.samskrtam.curriculum.mapper.LearnTopicMapperImpl;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.PrerequisiteStrength;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisite;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisiteId;
import sm.selflearn.samskrtam.curriculum.repository.TopicPrerequisiteRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearnGraphServiceTest {

    private static UUID id(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private static Topic topic(int n, String code, LearningLevel level, boolean evergreen) {
        Topic topic = new Topic();
        topic.setId(id(n));
        topic.setCode(code);
        topic.setTitleRu("Title " + n);
        topic.setTitleEn("Title " + n);
        topic.setLearningLevel(level);
        topic.setEvergreen(evergreen);
        topic.setDisplayOrder((short) n);
        return topic;
    }

    private static TopicPrerequisite edge(int topicId, int prerequisiteTopicId) {
        TopicPrerequisiteId key = new TopicPrerequisiteId();
        key.setTopicId(id(topicId));
        key.setPrerequisiteTopicId(id(prerequisiteTopicId));
        TopicPrerequisite edge = new TopicPrerequisite();
        edge.setId(key);
        edge.setStrength(PrerequisiteStrength.RECOMMENDED);
        return edge;
    }

    private LearnGraphService service(TopicRepository topicRepository, TopicPrerequisiteRepository prerequisiteRepository) {
        LearnTopicMapper mapper = new LearnTopicMapperImpl();
        return new LearnGraphService(topicRepository, prerequisiteRepository, mapper);
    }

    @Test
    void getLearnGraph_topicsByLevel_returnsSevenLayersAndEvergreenLast() {
        TopicRepository topicRepository = mock(TopicRepository.class);
        TopicPrerequisiteRepository prerequisiteRepository = mock(TopicPrerequisiteRepository.class);
        when(topicRepository.findAll()).thenReturn(List.of(
                topic(1, "a-stem-masc", LearningLevel.L1, false),
                topic(2, "deva-svara", LearningLevel.L0, false),
                topic(3, "mixed-review", LearningLevel.L0, true)));
        when(prerequisiteRepository.findAll()).thenReturn(List.of());

        LearnGraphResponse response = service(topicRepository, prerequisiteRepository).getLearnGraph();

        assertThat(response.layers()).hasSize(8);
        LearnLayerDto evergreen = response.layers().get(7);
        assertThat(evergreen.id()).isEqualTo("always");
        assertThat(evergreen.alwaysAvailable()).isTrue();
        assertThat(evergreen.topics()).extracting(LearnTopicDto::code).containsExactly("mixed-review");

        LearnLayerDto l0 = response.layers().get(0);
        assertThat(l0.id()).isEqualTo("L0");
        assertThat(l0.alwaysAvailable()).isFalse();
        assertThat(l0.topics()).extracting(LearnTopicDto::code).containsExactly("deva-svara");

        LearnLayerDto l1 = response.layers().get(1);
        assertThat(l1.id()).isEqualTo("L1");
        assertThat(l1.topics()).extracting(LearnTopicDto::code).containsExactly("a-stem-masc");
    }

    @Test
    void getLearnGraph_randomProgress_statusAndPercentAlwaysPresent() {
        TopicRepository topicRepository = mock(TopicRepository.class);
        TopicPrerequisiteRepository prerequisiteRepository = mock(TopicPrerequisiteRepository.class);
        when(topicRepository.findAll()).thenReturn(List.of(
                topic(1, "a-stem-masc", LearningLevel.L1, false),
                topic(2, "deva-svara", LearningLevel.L0, false)));
        when(prerequisiteRepository.findAll()).thenReturn(List.of());

        LearnGraphResponse response = service(topicRepository, prerequisiteRepository).getLearnGraph();

        List<LearnTopicDto> topics = response.layers().stream()
                .flatMap(layer -> layer.topics().stream())
                .toList();
        assertThat(topics).hasSize(2);
        for (LearnTopicDto topic : topics) {
            assertThat(topic.status()).isNotNull();
            assertThat(topic.progressPercent()).isNotNull();
            assertThat(topic.progressPercent()).isBetween(0, 100);
        }
    }

    @Test
    void getLearnGraph_prerequisiteEdge_resolvesPrerequisiteCode() {
        TopicRepository topicRepository = mock(TopicRepository.class);
        TopicPrerequisiteRepository prerequisiteRepository = mock(TopicPrerequisiteRepository.class);
        when(topicRepository.findAll()).thenReturn(List.of(
                topic(1, "a-stem-masc", LearningLevel.L1, false),
                topic(2, "stem-case-concept", LearningLevel.L1, false)));
        when(prerequisiteRepository.findAll()).thenReturn(List.of(edge(1, 2)));

        LearnGraphResponse response = service(topicRepository, prerequisiteRepository).getLearnGraph();

        LearnTopicDto aStem = response.layers().stream()
                .flatMap(layer -> layer.topics().stream())
                .filter(topic -> topic.code().equals("a-stem-masc"))
                .findFirst()
                .orElseThrow();
        assertThat(aStem.prerequisites()).containsExactly("stem-case-concept");
    }

    @Test
    void getLearnGraph_sandhiCode_classifiedAsSandhi() {
        TopicRepository topicRepository = mock(TopicRepository.class);
        TopicPrerequisiteRepository prerequisiteRepository = mock(TopicPrerequisiteRepository.class);
        when(topicRepository.findAll()).thenReturn(List.of(
                topic(1, "sandhi-consonants", LearningLevel.L1, false)));
        when(prerequisiteRepository.findAll()).thenReturn(List.of());

        LearnGraphResponse response = service(topicRepository, prerequisiteRepository).getLearnGraph();

        LearnTopicDto topic = response.layers().stream()
                .flatMap(layer -> layer.topics().stream())
                .findFirst()
                .orElseThrow();
        assertThat(topic.typeGroup().name()).isEqualTo("SANDHI");
    }
}