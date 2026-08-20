package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.ReferenceClassDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.SemanticClassNodeDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.SemanticClassUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.model.FrequencyBand;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyAppliesTo;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PosGroup;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticClass;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.FrequencyBandRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticClassRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Admin CRUD справочников lexicon (task-curriculum-16 §6): SemanticClass,
 * PartOfSpeech, MorphologyClass, FrequencyBand. Запись — ADMIN, чтение публичное.
 */
@Service
@RequiredArgsConstructor
public class LexiconReferenceService {

    private final SemanticClassRepository semanticClassRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final MorphologyClassRepository morphologyClassRepository;
    private final FrequencyBandRepository frequencyBandRepository;

    // --- SemanticClass ---

    @Transactional(readOnly = true)
    public List<SemanticClassNodeDto> semanticClassTree() {
        List<SemanticClass> roots = semanticClassRepository.findByParentIsNull()
                .stream()
                .sorted(Comparator.comparing(SemanticClass::getCode))
                .toList();
        return roots.stream().map(root -> toNode(root, new java.util.HashSet<>())).toList();
    }

    @Transactional
    public SemanticClassNodeDto createSemanticClass(SemanticClassUpsertRequest request) {
        SemanticClass topic = new SemanticClass();
        apply(topic, request);
        return toNode(semanticClassRepository.save(topic), new java.util.HashSet<>());
    }

    @Transactional
    public SemanticClassNodeDto updateSemanticClass(UUID id, SemanticClassUpsertRequest request) {
        SemanticClass topic = semanticClassRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SemanticClass not found"));
        apply(topic, request);
        return toNode(semanticClassRepository.save(topic), new java.util.HashSet<>());
    }

    @Transactional
    public void deleteSemanticClass(UUID id) {
        if (!semanticClassRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SemanticClass not found");
        }
        semanticClassRepository.deleteById(id);
    }

    private void apply(SemanticClass topic, SemanticClassUpsertRequest request) {
        topic.setCode(request.code());
        topic.setNameRu(request.nameRu());
        topic.setNameEn(request.nameEn());
        if (request.parentId() != null) {
            topic.setParent(semanticClassRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent not found")));
        } else {
            topic.setParent(null);
        }
    }

    private SemanticClassNodeDto toNode(SemanticClass topic, java.util.Set<UUID> seen) {
        List<SemanticClassNodeDto> children = new ArrayList<>();
        if (seen.add(topic.getId())) {
            children = topic.getChildren().stream()
                    .sorted(Comparator.comparing(SemanticClass::getCode))
                    .map(child -> toNode(child, seen))
                    .toList();
        }
        return new SemanticClassNodeDto(
                topic.getId(), topic.getCode(), topic.getNameRu(), topic.getNameEn(),
                topic.getParent() == null ? null : topic.getParent().getId(), children);
    }

    // --- PartOfSpeech ---

    @Transactional(readOnly = true)
    public List<ReferenceClassDto> listPos() {
        return partOfSpeechRepository.findAll().stream()
                .sorted(Comparator.comparing(PartOfSpeech::getCode))
                .map(p -> ReferenceClassDto.pos(p.getCode(), p.getGroup(), p.getNameRu(), p.getNameEn()))
                .toList();
    }

    @Transactional
    public ReferenceClassDto upsertPos(ReferenceClassDto dto) {
        PartOfSpeech pos = partOfSpeechRepository.findById(dto.code()).orElse(new PartOfSpeech());
        pos.setCode(dto.code());
        pos.setGroup(PosGroup.valueOf(dto.group()));
        pos.setNameRu(dto.nameRu());
        pos.setNameEn(dto.nameEn());
        return ReferenceClassDto.pos(partOfSpeechRepository.save(pos).getCode(),
                pos.getGroup(), pos.getNameRu(), pos.getNameEn());
    }

    @Transactional
    public void deletePos(String code) {
        if (!partOfSpeechRepository.existsById(code)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PartOfSpeech not found");
        }
        partOfSpeechRepository.deleteById(code);
    }

    // --- MorphologyClass ---

    @Transactional(readOnly = true)
    public List<ReferenceClassDto> listMorphologyClasses() {
        return morphologyClassRepository.findAll().stream()
                .sorted(Comparator.comparing(MorphologyClass::getCode))
                .map(m -> ReferenceClassDto.morphology(m.getCode(), m.getAppliesTo(), m.getNameRu(), m.getNameEn()))
                .toList();
    }

    @Transactional
    public ReferenceClassDto upsertMorphologyClass(ReferenceClassDto dto) {
        MorphologyClass mc = morphologyClassRepository.findById(dto.code()).orElse(new MorphologyClass());
        mc.setCode(dto.code());
        mc.setAppliesTo(dto.appliesTo());
        mc.setNameRu(dto.nameRu());
        mc.setNameEn(dto.nameEn());
        MorphologyClass saved = morphologyClassRepository.save(mc);
        return ReferenceClassDto.morphology(saved.getCode(), saved.getAppliesTo(),
                saved.getNameRu(), saved.getNameEn());
    }

    @Transactional
    public void deleteMorphologyClass(String code) {
        if (!morphologyClassRepository.existsById(code)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "MorphologyClass not found");
        }
        morphologyClassRepository.deleteById(code);
    }

    // --- FrequencyBand ---

    @Transactional(readOnly = true)
    public List<FrequencyBand> listBands() {
        return frequencyBandRepository.findAllByOrderBySortOrderAsc();
    }

    @Transactional
    public FrequencyBand upsertBand(FrequencyBand band) {
        if (band.getSortOrder() == null) {
            band.setSortOrder((short) 0);
        }
        return frequencyBandRepository.save(band);
    }

    @Transactional
    public void deleteBand(String code) {
        if (!frequencyBandRepository.existsById(code)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "FrequencyBand not found");
        }
        frequencyBandRepository.deleteById(code);
    }
}