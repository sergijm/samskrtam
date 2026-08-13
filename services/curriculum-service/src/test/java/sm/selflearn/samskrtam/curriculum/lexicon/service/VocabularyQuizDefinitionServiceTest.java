package sm.selflearn.samskrtam.curriculum.lexicon.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.VocabularyQuizDefinitionUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.model.VocabularyQuizKind;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.VocabularyQuizDefinitionRepository;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuiz;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.repository.ComplexQuizRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VocabularyQuizDefinitionServiceTest {

    private VocabularyQuizDefinitionService service(
            VocabularyQuizDefinitionRepository definitionRepo,
            TopicRepository topicRepo,
            ComplexQuizRepository complexQuizRepo) {
        return new VocabularyQuizDefinitionService(
                definitionRepo, topicRepo, complexQuizRepo);
    }

    @Test
    void create_moreThanOneExclusiveField_throws422() {
        VocabularyQuizDefinitionRepository definitionRepo =
                mock(VocabularyQuizDefinitionRepository.class);
        UUID topicId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();

        TopicRepository topicRepo = mock(TopicRepository.class);
        when(topicRepo.findById(topicId)).thenReturn(Optional.of(new Topic()));
        ComplexQuizRepository complexQuizRepo = mock(ComplexQuizRepository.class);
        when(complexQuizRepo.findById(quizId)).thenReturn(Optional.of(new ComplexQuiz()));

        VocabularyQuizDefinitionUpsertRequest request =
                new VocabularyQuizDefinitionUpsertRequest(
                        VocabularyQuizKind.TOPIC, "t", "t", topicId, quizId, null);

        VocabularyQuizDefinitionService svc =
                service(definitionRepo, topicRepo, complexQuizRepo);

        assertThatThrownBy(() -> svc.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(422);
        verify(definitionRepo, org.mockito.Mockito.never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void create_exactlyOneField_saves() {
        VocabularyQuizDefinitionRepository definitionRepo =
                mock(VocabularyQuizDefinitionRepository.class);
        when(definitionRepo.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));

        VocabularyQuizDefinitionUpsertRequest request =
                new VocabularyQuizDefinitionUpsertRequest(
                        VocabularyQuizKind.FREQUENCY_BAND, "Core", "Core",
                        null, null, 100);

        VocabularyQuizDefinitionService svc =
                service(definitionRepo, mock(TopicRepository.class),
                        mock(ComplexQuizRepository.class));

        var saved = svc.create(request);

        assertThat(saved.getKind()).isEqualTo(VocabularyQuizKind.FREQUENCY_BAND);
        assertThat(saved.getFrequencyRankMax()).isEqualTo(100);
    }
}