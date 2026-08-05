package sm.selflearn.samskrtam.curriculum.service;

import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.exception.InvalidComplexQuizCompositionException;
import sm.selflearn.samskrtam.curriculum.mapper.ComplexQuizMapper;
import sm.selflearn.samskrtam.curriculum.mapper.TopicMapper;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuiz;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizTopic;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizTopicId;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuizType;
import sm.selflearn.samskrtam.curriculum.model.LearningLevel;
import sm.selflearn.samskrtam.curriculum.repository.ComplexQuizRepository;
import sm.selflearn.samskrtam.curriculum.repository.ComplexQuizTopicRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComplexQuizServiceTest {

    private static UUID id(int n) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", n));
    }

    private static ComplexQuizService service(
            ComplexQuizRepository quizRepository,
            ComplexQuizTopicRepository quizTopicRepository,
            TopicRepository topicRepository) {
        return new ComplexQuizService(
                quizRepository, quizTopicRepository, topicRepository,
                mock(ComplexQuizMapper.class), mock(TopicMapper.class));
    }

    @Test
    void validateComposition_duplicateIds_throwsInvalidComposition() {
        TopicRepository topics = mock(TopicRepository.class);
        ComplexQuizService svc = service(mock(ComplexQuizRepository.class), mock(ComplexQuizTopicRepository.class), topics);

        assertThatThrownBy(() -> svc.validateComposition(
                ComplexQuizType.MIXED_PRACTICE, List.of(id(1), id(1), id(2))))
                .isInstanceOf(InvalidComplexQuizCompositionException.class)
                .hasMessageContaining("unique");
    }

    @Test
    void validateComposition_mixedPracticeTooFew_throwsInvalidComposition() {
        TopicRepository topics = mock(TopicRepository.class);
        when(topics.existsById(id(1))).thenReturn(true);
        ComplexQuizService svc = service(mock(ComplexQuizRepository.class), mock(ComplexQuizTopicRepository.class), topics);

        assertThatThrownBy(() -> svc.validateComposition(
                ComplexQuizType.MIXED_PRACTICE, List.of(id(1))))
                .isInstanceOf(InvalidComplexQuizCompositionException.class)
                .hasMessageContaining("2-4");
    }

    @Test
    void validateComposition_levelAssessmentOutsideRange_throwsInvalidComposition() {
        TopicRepository topics = mock(TopicRepository.class);
        when(topics.existsById(id(1))).thenReturn(true);
        when(topics.existsById(id(2))).thenReturn(true);
        when(topics.existsById(id(3))).thenReturn(true);
        when(topics.existsById(id(4))).thenReturn(true);
        ComplexQuizService svc = service(mock(ComplexQuizRepository.class), mock(ComplexQuizTopicRepository.class), topics);

        assertThatThrownBy(() -> svc.validateComposition(
                ComplexQuizType.LEVEL_ASSESSMENT, List.of(id(1), id(2), id(3), id(4))))
                .isInstanceOf(InvalidComplexQuizCompositionException.class)
                .hasMessageContaining("5-7");
    }

    @Test
    void validateComposition_unknownTopic_throwsEntityNotFound() {
        TopicRepository topics = mock(TopicRepository.class);
        when(topics.existsById(id(1))).thenReturn(true);
        when(topics.existsById(id(2))).thenReturn(false);
        ComplexQuizService svc = service(mock(ComplexQuizRepository.class), mock(ComplexQuizTopicRepository.class), topics);

        assertThatThrownBy(() -> svc.validateComposition(
                ComplexQuizType.MIXED_PRACTICE, List.of(id(1), id(2))))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
    }

    @Test
    void validateComposition_validMixedPractice_noException() {
        TopicRepository topics = mock(TopicRepository.class);
        when(topics.existsById(id(1))).thenReturn(true);
        when(topics.existsById(id(2))).thenReturn(true);
        ComplexQuizService svc = service(mock(ComplexQuizRepository.class), mock(ComplexQuizTopicRepository.class), topics);

        svc.validateComposition(ComplexQuizType.MIXED_PRACTICE, List.of(id(1), id(2)));
    }

    @Test
    void resolveAppearsInLevels_topicInQuizzes_returnsOwnLevelThenDistinctSorted() {
        ComplexQuizTopicRepository quizTopics = mock(ComplexQuizTopicRepository.class);
        ComplexQuizRepository quizzes = mock(ComplexQuizRepository.class);

        ComplexQuizTopic rowL2 = new ComplexQuizTopic();
        rowL2.setId(key(100, 1));
        ComplexQuizTopic rowL4 = new ComplexQuizTopic();
        rowL4.setId(key(101, 1));
        when(quizTopics.findByIdTopicId(id(1))).thenReturn(List.of(rowL2, rowL4));

        ComplexQuiz quizL4 = new ComplexQuiz();
        quizL4.setLearningLevel(LearningLevel.L4);
        ComplexQuiz quizL2 = new ComplexQuiz();
        quizL2.setLearningLevel(LearningLevel.L2);
        when(quizzes.findAllById(List.of(id(100), id(101)))).thenReturn(List.of(quizL4, quizL2));

        ComplexQuizService svc = service(quizzes, quizTopics, mock(TopicRepository.class));

        assertThat(svc.resolveAppearsInLevels(id(1), LearningLevel.L0))
                .containsExactly(LearningLevel.L0, LearningLevel.L2, LearningLevel.L4);
    }

    @Test
    void resolveAppearsInLevels_ownLevelAlsoInQuizzes_noDuplicate() {
        ComplexQuizTopicRepository quizTopics = mock(ComplexQuizTopicRepository.class);
        ComplexQuizRepository quizzes = mock(ComplexQuizRepository.class);

        ComplexQuizTopic row = new ComplexQuizTopic();
        row.setId(key(100, 1));
        when(quizTopics.findByIdTopicId(id(1))).thenReturn(List.of(row));

        ComplexQuiz quiz = new ComplexQuiz();
        quiz.setLearningLevel(LearningLevel.L2);
        when(quizzes.findAllById(List.of(id(100)))).thenReturn(List.of(quiz));

        ComplexQuizService svc = service(quizzes, quizTopics, mock(TopicRepository.class));

        assertThat(svc.resolveAppearsInLevels(id(1), LearningLevel.L2))
                .containsExactly(LearningLevel.L2);
    }

    @Test
    void resolveAppearsInLevels_noQuizzes_returnsOwnLevelOnly() {
        ComplexQuizTopicRepository quizTopics = mock(ComplexQuizTopicRepository.class);
        when(quizTopics.findByIdTopicId(id(1))).thenReturn(List.of());

        ComplexQuizService svc = service(
                mock(ComplexQuizRepository.class), quizTopics, mock(TopicRepository.class));

        assertThat(svc.resolveAppearsInLevels(id(1), LearningLevel.L3))
                .containsExactly(LearningLevel.L3);
    }

    private static ComplexQuizTopicId key(int quizId, int topicId) {
        ComplexQuizTopicId key = new ComplexQuizTopicId();
        key.setComplexQuizId(id(quizId));
        key.setTopicId(id(topicId));
        return key;
    }
}
