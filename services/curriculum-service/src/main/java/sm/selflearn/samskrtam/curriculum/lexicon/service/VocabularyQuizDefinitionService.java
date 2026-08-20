package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.VocabularyQuizDefinitionUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.model.VocabularyQuizDefinition;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.VocabularyQuizDefinitionRepository;
import sm.selflearn.samskrtam.curriculum.model.ComplexQuiz;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.repository.ComplexQuizRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.UUID;

/**
 * CRUD определений вок. викторин (task-curriculum-16 §10): ровно одно из
 * четырёх взаимоисключающих полей (topic/complexQuiz/source/frequencyRankMax).
 */
@Service
@RequiredArgsConstructor
public class VocabularyQuizDefinitionService {

    private final VocabularyQuizDefinitionRepository definitionRepository;
    private final TopicRepository topicRepository;
    private final ComplexQuizRepository complexQuizRepository;

    @Transactional(readOnly = true)
    public VocabularyQuizDefinition get(UUID id) {
        return definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Definition not found"));
    }

    @Transactional
    public VocabularyQuizDefinition create(VocabularyQuizDefinitionUpsertRequest request) {
        VocabularyQuizDefinition definition = new VocabularyQuizDefinition();
        apply(definition, request);
        return definitionRepository.save(definition);
    }

    @Transactional
    public VocabularyQuizDefinition update(UUID id, VocabularyQuizDefinitionUpsertRequest request) {
        VocabularyQuizDefinition definition = definitionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Definition not found"));
        apply(definition, request);
        return definitionRepository.save(definition);
    }

    @Transactional
    public void delete(UUID id) {
        if (!definitionRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Definition not found");
        }
        definitionRepository.deleteById(id);
    }

    private void apply(VocabularyQuizDefinition definition,
                       VocabularyQuizDefinitionUpsertRequest request) {
        validateExclusiveFields(request);
        definition.setKind(request.kind());
        definition.setTitleRu(request.titleRu());
        definition.setTitleEn(request.titleEn());
        definition.setTopic(resolve(request.topicId(), Topic.class, topicRepository::findById));
        definition.setComplexQuiz(resolve(request.complexQuizId(), ComplexQuiz.class,
                complexQuizRepository::findById));
        definition.setFrequencyRankMax(request.frequencyRankMax());
    }

    private void validateExclusiveFields(VocabularyQuizDefinitionUpsertRequest request) {
        int filled = 0;
        if (request.topicId() != null) filled++;
        if (request.complexQuizId() != null) filled++;
        if (request.frequencyRankMax() != null) filled++;
        if (filled != 1) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Exactly one of topicId/complexQuizId/frequencyRankMax must be set");
        }
    }

    private <T> T resolve(UUID id, Class<T> type,
                          java.util.function.Function<UUID, java.util.Optional<T>> finder) {
        if (id == null) {
            return null;
        }
        return finder.apply(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        type.getSimpleName() + " not found"));
    }
}