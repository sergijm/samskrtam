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
import sm.selflearn.samskrtam.curriculum.lexicon.dto.ReferenceClassDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.SemanticClassNodeDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.SemanticClassUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.service.LexiconReferenceService;

import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD справочников lexicon (task-curriculum-16 §6). Запись — ADMIN,
 * чтение публичное.
 */
@RestController
@RequestMapping("/api/v2/lexicon")
@RequiredArgsConstructor
public class LexiconReferenceController {

    private final LexiconReferenceService referenceService;

    // SemanticClass
    @GetMapping("/semantic-classes/tree")
    public List<SemanticClassNodeDto> semanticClassTree() {
        return referenceService.semanticClassTree();
    }

    @PostMapping("/semantic-classes")
    public SemanticClassNodeDto createSemanticClass(@RequestBody SemanticClassUpsertRequest request) {
        return referenceService.createSemanticClass(request);
    }

    @PutMapping("/semantic-classes/{id}")
    public SemanticClassNodeDto updateSemanticClass(@PathVariable UUID id,
                                                    @RequestBody SemanticClassUpsertRequest request) {
        return referenceService.updateSemanticClass(id, request);
    }

    @DeleteMapping("/semantic-classes/{id}")
    public void deleteSemanticClass(@PathVariable UUID id) {
        referenceService.deleteSemanticClass(id);
    }

    // PartOfSpeech
    @GetMapping("/pos")
    public List<ReferenceClassDto> listPos() {
        return referenceService.listPos();
    }

    @PutMapping("/pos")
    public ReferenceClassDto upsertPos(@RequestBody ReferenceClassDto dto) {
        return referenceService.upsertPos(dto);
    }

    @DeleteMapping("/pos/{code}")
    public void deletePos(@PathVariable String code) {
        referenceService.deletePos(code);
    }

    // MorphologyClass
    @GetMapping("/morphology-classes")
    public List<ReferenceClassDto> listMorphologyClasses() {
        return referenceService.listMorphologyClasses();
    }

    @PutMapping("/morphology-classes")
    public ReferenceClassDto upsertMorphologyClass(@RequestBody ReferenceClassDto dto) {
        return referenceService.upsertMorphologyClass(dto);
    }

    @DeleteMapping("/morphology-classes/{code}")
    public void deleteMorphologyClass(@PathVariable String code) {
        referenceService.deleteMorphologyClass(code);
    }

    // FrequencyBand
    @GetMapping("/frequency-bands")
    public List<FrequencyBand> listBands() {
        return referenceService.listBands();
    }

    @PutMapping("/frequency-bands")
    public FrequencyBand upsertBand(@RequestBody FrequencyBand band) {
        return referenceService.upsertBand(band);
    }

    @DeleteMapping("/frequency-bands/{code}")
    public void deleteBand(@PathVariable String code) {
        referenceService.deleteBand(code);
    }
}