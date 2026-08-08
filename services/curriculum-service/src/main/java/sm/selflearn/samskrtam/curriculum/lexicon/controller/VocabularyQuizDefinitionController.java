package sm.selflearn.samskrtam.curriculum.lexicon.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.VocabularyQuizDefinitionUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.model.VocabularyQuizDefinition;
import sm.selflearn.samskrtam.curriculum.lexicon.model.VocabularyQuizKind;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.VocabularyQuizDefinitionRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.service.VocabularyQuizDefinitionService;

import java.util.List;
import java.util.UUID;

/**
 * Определения вок. викторин (task-curriculum-16 §10): запись ADMIN, GET публичный.
 */
@RestController
@RequestMapping("/api/v2/lexicon/vocabulary-quiz-definitions")
@RequiredArgsConstructor
public class VocabularyQuizDefinitionController {

    private final VocabularyQuizDefinitionService definitionService;
    private final VocabularyQuizDefinitionRepository definitionRepository;

    @GetMapping
    public List<VocabularyQuizDefinition> list(
            @org.springframework.web.bind.annotation.RequestParam(required = false) VocabularyQuizKind kind) {
        return kind == null
                ? definitionRepository.findAll()
                : definitionRepository.findByKind(kind);
    }

    @GetMapping("/{id}")
    public VocabularyQuizDefinition get(@PathVariable UUID id) {
        return definitionService.get(id);
    }

    @PostMapping
    public VocabularyQuizDefinition create(@RequestBody VocabularyQuizDefinitionUpsertRequest request) {
        return definitionService.create(request);
    }

    @PutMapping("/{id}")
    public VocabularyQuizDefinition update(@PathVariable UUID id,
                                           @RequestBody VocabularyQuizDefinitionUpsertRequest request) {
        return definitionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable UUID id) {
        definitionService.delete(id);
    }
}