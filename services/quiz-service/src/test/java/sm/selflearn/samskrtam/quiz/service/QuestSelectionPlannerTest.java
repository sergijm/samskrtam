package sm.selflearn.samskrtam.quiz.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QuestSelectionPlannerTest {

    @Test
    void interleavesGroupsRoundRobinUpToLimit() {
        List<List<String>> groups = List.of(List.of("a1", "a2", "a3"), List.of("b1", "b2"), List.of("c1"));
        List<String> picked = QuestSelectionPlanner.takeRoundRobin(groups, 4);
        assertThat(picked).containsExactly("a1", "b1", "c1", "a2");
    }

    @Test
    void limitRespectsGroupSizes() {
        List<List<String>> groups = List.of(List.of("a1"), List.of("b1", "b2"));
        assertThat(QuestSelectionPlanner.takeRoundRobin(groups, 10)).hasSize(3);
    }

    @Test
    void emptyGroupsOrLimitYieldsEmpty() {
        assertThat(QuestSelectionPlanner.takeRoundRobin(List.of(), 5)).isEmpty();
        assertThat(QuestSelectionPlanner.takeRoundRobin(List.of(List.of("x")), 0)).isEmpty();
    }
}
