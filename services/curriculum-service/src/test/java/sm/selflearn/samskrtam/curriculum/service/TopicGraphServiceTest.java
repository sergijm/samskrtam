package sm.selflearn.samskrtam.curriculum.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.model.PrerequisiteStrength;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisite;
import sm.selflearn.samskrtam.curriculum.model.TopicPrerequisiteId;
import sm.selflearn.samskrtam.curriculum.repository.TopicPrerequisiteRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TopicGraphServiceTest {

    private static UUID id(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private static Topic topic(int n, String titleRu) {
        return topic(n, titleRu, null, false);
    }

    private static Topic topic(int n, String titleRu, Short displayOrder, boolean evergreen) {
        Topic topic = new Topic();
        topic.setId(id(n));
        topic.setCode("topic-" + n);
        topic.setTitleRu(titleRu);
        topic.setTitleEn(titleRu);
        topic.setLearningLevel(LearningLevel.L0);
        topic.setDisplayOrder(displayOrder);
        topic.setEvergreen(evergreen);
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

    @Test
    void computeLayers_linearChain_threeLayers() {
        Topic a = topic(1, "A");
        Topic b = topic(2, "B");
        Topic c = topic(3, "C");
        List<Topic> topics = List.of(a, b, c);
        List<TopicPrerequisite> edges = List.of(edge(2, 1), edge(3, 2));

        TopicGraphService.TopicGraphResult result = new TopicGraphService(mock(TopicPrerequisiteRepository.class))
                .computeLayers(topics, edges);

        assertThat(result.layers().keySet()).containsExactly(0, 1, 2);
        assertThat(result.layers().get(0)).containsExactly(a);
        assertThat(result.layers().get(1)).containsExactly(b);
        assertThat(result.layers().get(2)).containsExactly(c);
        assertThat(result.evergreen()).isEmpty();
    }

    @Test
    void computeLayers_independentTopics_allInLayerZero() {
        Topic a = topic(1, "A");
        Topic b = topic(2, "B");
        Topic c = topic(3, "C");

        TopicGraphService.TopicGraphResult result = new TopicGraphService(mock(TopicPrerequisiteRepository.class))
                .computeLayers(List.of(a, b, c), List.of());

        assertThat(result.layers().keySet()).containsExactly(0);
        assertThat(result.layers().get(0)).containsExactlyInAnyOrder(a, b, c);
    }

    @Test
    void computeLayers_diamondGraph_dInLayerTwo() {
        Topic a = topic(1, "A");
        Topic b = topic(2, "B");
        Topic c = topic(3, "C");
        Topic d = topic(4, "D");
        List<Topic> topics = List.of(a, b, c, d);
        List<TopicPrerequisite> edges = List.of(edge(2, 1), edge(3, 1), edge(4, 2), edge(4, 3));

        TopicGraphService.TopicGraphResult result = new TopicGraphService(mock(TopicPrerequisiteRepository.class))
                .computeLayers(topics, edges);

        assertThat(result.layers().get(0)).containsExactly(a);
        assertThat(result.layers().get(1)).containsExactlyInAnyOrder(b, c);
        assertThat(result.layers().get(2)).containsExactly(d);
    }

    @Test
    void computeLayers_cycleInData_throwsIllegalStateException() {
        Topic a = topic(1, "A");
        Topic b = topic(2, "B");
        List<TopicPrerequisite> edges = List.of(edge(2, 1), edge(1, 2));

        assertThatThrownBy(() -> new TopicGraphService(mock(TopicPrerequisiteRepository.class))
                .computeLayers(List.of(a, b), edges))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cycle");
    }

    @Test
    void computeLayers_evergreenTopics_notInAnyLayer() {
        Topic evergreen = topic(1, "Mixed review", null, true);
        Topic b = topic(2, "B");
        Topic c = topic(3, "C");
        List<TopicPrerequisite> edges = List.of(edge(3, 2));

        TopicGraphService.TopicGraphResult result = new TopicGraphService(mock(TopicPrerequisiteRepository.class))
                .computeLayers(List.of(evergreen, b, c), edges);

        assertThat(result.layers().keySet()).containsExactly(0, 1);
        assertThat(result.layers().get(0)).containsExactly(b);
        assertThat(result.layers().get(1)).containsExactly(c);
        assertThat(result.evergreen()).containsExactly(evergreen);
    }

    @Test
    void computeLayers_withinLayer_sortedByDisplayOrderThenTitleRu() {
        Topic first = topic(1, "b", (short) 1, false);
        Topic second = topic(2, "c", (short) 2, false);
        Topic third = topic(3, "a", null, false);
        Topic fourth = topic(4, "aa", null, false);

        TopicGraphService.TopicGraphResult result = new TopicGraphService(mock(TopicPrerequisiteRepository.class))
                .computeLayers(List.of(first, second, third, fourth), List.of());

        assertThat(result.layers().get(0)).containsExactly(first, second, third, fourth);
    }

    @Test
    void wouldCreateCycle_selfLoop_returnsTrue() {
        assertThat(new TopicGraphService(mock(TopicPrerequisiteRepository.class))
                .wouldCreateCycle(id(1), id(1))).isTrue();
    }

    @Test
    void wouldCreateCycle_pathFromPrerequisiteBackToTopic_returnsTrue() {
        TopicPrerequisiteRepository repository = mock(TopicPrerequisiteRepository.class);
        when(repository.findByIdTopicId(id(3))).thenReturn(List.of(edge(3, 2)));
        when(repository.findByIdTopicId(id(2))).thenReturn(List.of(edge(2, 1)));

        assertThat(new TopicGraphService(repository).wouldCreateCycle(id(1), id(3))).isTrue();
    }

    @Test
    void wouldCreateCycle_noPathToTopic_returnsFalse() {
        TopicPrerequisiteRepository repository = mock(TopicPrerequisiteRepository.class);
        when(repository.findByIdTopicId(id(2))).thenReturn(List.of(edge(2, 1)));
        when(repository.findByIdTopicId(id(1))).thenReturn(List.of());

        assertThat(new TopicGraphService(repository).wouldCreateCycle(id(3), id(2))).isFalse();
    }
}
