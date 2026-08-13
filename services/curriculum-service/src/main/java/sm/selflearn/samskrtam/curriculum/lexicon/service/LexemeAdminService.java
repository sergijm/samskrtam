package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeAdminDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeAdminPage;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeCandidateDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeDetailDto;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.LexemeUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.imports.LexiconImportService;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexemeGender;
import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyClass;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PartOfSpeech;
import sm.selflearn.samskrtam.curriculum.lexicon.model.SemanticTopic;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeFrequencyRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.MorphologyClassRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.PartOfSpeechRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.SemanticTopicRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ADMIN-CRUD лексем (task-curriculum-16 §1–§4).
 */
@Service
@RequiredArgsConstructor
public class LexemeAdminService {

    private final LexemeRepository lexemeRepository;
    private final LexemeFrequencyRepository frequencyRepository;
    private final PartOfSpeechRepository partOfSpeechRepository;
    private final MorphologyClassRepository morphologyClassRepository;
    private final SemanticTopicRepository semanticTopicRepository;

    @Transactional(readOnly = true)
    public LexemeAdminPage list(String posCode, UUID semanticTopicId,
                                boolean noSemanticTopic, int page, int size) {
        Page<Lexeme> result = lexemeRepository.search(
                posCode, semanticTopicId, noSemanticTopic, PageRequest.of(page, size));
        List<LexemeAdminDto> items = result.getContent().stream()
                .map(this::toAdminDto)
                .toList();
        return new LexemeAdminPage(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public LexemeDetailDto get(UUID id) {
        Lexeme lexeme = lexemeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lexeme not found"));
        return toDetailDto(lexeme);
    }

    @Transactional
    public LexemeDetailDto create(LexemeUpsertRequest request) {
        Lexeme lexeme = new Lexeme();
        apply(lexeme, request);
        lexemeRepository.save(lexeme);
        replaceTaxonomies(lexeme, request.posCodes(), request.morphologyClassCodes(),
                request.semanticTopicIds());
        return toDetailDto(lexeme);
    }

    @Transactional
    public LexemeDetailDto update(UUID id, LexemeUpsertRequest request) {
        Lexeme lexeme = lexemeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lexeme not found"));
        apply(lexeme, request);
        replaceTaxonomies(lexeme, request.posCodes(), request.morphologyClassCodes(),
                request.semanticTopicIds());
        return toDetailDto(lexeme);
    }

    /**
     * Идемпотентная замена набора семантических тем (task-curriculum-16 §5).
     */
    @Transactional
    public LexemeDetailDto replaceSemanticTopics(UUID id, List<UUID> topicIds) {
        Lexeme lexeme = lexemeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lexeme not found"));
        Set<SemanticTopic> topics = new HashSet<>();
        if (topicIds != null) {
            for (UUID topicId : topicIds) {
                semanticTopicRepository.findById(topicId)
                        .ifPresent(topics::add);
            }
        }
        lexeme.setSemanticTopics(topics);
        lexemeRepository.save(lexeme);
        return toDetailDto(lexeme);
    }

    @Transactional
    public LexemeDetailDto replacePos(UUID id, List<String> posCodes) {
        Lexeme lexeme = lexemeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lexeme not found"));
        lexeme.setPartsOfSpeech(resolve(posCodes, partOfSpeechRepository::findByCode));
        lexemeRepository.save(lexeme);
        return toDetailDto(lexeme);
    }

    @Transactional
    public LexemeDetailDto replaceMorphology(UUID id, List<String> codes) {
        Lexeme lexeme = lexemeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lexeme not found"));
        lexeme.setMorphologyClasses(resolve(codes, morphologyClassRepository::findByCode));
        lexemeRepository.save(lexeme);
        return toDetailDto(lexeme);
    }

    private void apply(Lexeme lexeme, LexemeUpsertRequest request) {
        if (request.lemmaIast() != null) {
            lexeme.setLemmaIast(request.lemmaIast());
        }
        if (request.lemmaDevanagari() != null) {
            lexeme.setLemmaDevanagari(request.lemmaDevanagari());
        }
        if (request.lemmaSlp1() != null) {
            lexeme.setLemmaSlp1(request.lemmaSlp1());
        }
        lexeme.setGlossRu(nullToEmpty(request.glossRu()));
        lexeme.setGlossEn(nullToEmpty(request.glossEn()));
        lexeme.setLongDefinitionRu(request.longDefinitionRu());
        lexeme.setLongDefinitionEn(request.longDefinitionEn());
        lexeme.setGender(request.gender());
    }

    private void replaceTaxonomies(Lexeme lexeme, List<String> posCodes,
                                   List<String> morphologyCodes, List<UUID> topicIds) {
        if (posCodes != null) {
            lexeme.setPartsOfSpeech(resolve(posCodes, partOfSpeechRepository::findByCode));
        }
        if (morphologyCodes != null) {
            lexeme.setMorphologyClasses(resolve(morphologyCodes, morphologyClassRepository::findByCode));
        }
        if (topicIds != null) {
            lexeme.setSemanticTopics(SemanticTopicSet(topicIds));
        }
    }

    private Set<SemanticTopic> SemanticTopicSet(List<UUID> topicIds) {
        Set<SemanticTopic> topics = new HashSet<>();
        for (UUID id : topicIds) {
            semanticTopicRepository.findById(id).ifPresent(topics::add);
        }
        return topics;
    }

    private <T> Set<T> resolve(List<String> codes, java.util.function.Function<String, java.util.Optional<T>> resolver) {
        Set<T> result = new HashSet<>();
        if (codes != null) {
            for (String code : codes) {
                resolver.apply(code).ifPresent(result::add);
            }
        }
        return result;
    }

    private LexemeAdminDto toAdminDto(Lexeme lexeme) {
        Integer rank = frequencyRepository
                .findByIdLexemeIdAndIdSource(lexeme.getId(), LexiconImportService.FREQUENCY_SOURCE)
                .map(f -> f.getRank())
                .orElse(null);
        return new LexemeAdminDto(
                lexeme.getId(),
                lexeme.getLemmaIast(),
                lexeme.getLemmaDevanagari(),
                lexeme.getLemmaSlp1(),
                lexeme.getGlossRu(),
                lexeme.getGlossEn(),
                lexeme.getGender() == null ? null : lexeme.getGender().name(),
                rank,
                lexeme.getWordForms().size(),
                !lexeme.getSemanticTopics().isEmpty());
    }

    private LexemeDetailDto toDetailDto(Lexeme lexeme) {
        List<LexemeCandidateDto.WordFormDto> wordForms = lexeme.getWordForms().stream()
                .map(wf -> new LexemeCandidateDto.WordFormDto(
                        wf.getFormIast(), wf.getFormDevanagari(), wf.getGrammaticalNote()))
                .toList();
        return new LexemeDetailDto(
                lexeme.getId(),
                lexeme.getLemmaIast(),
                lexeme.getLemmaDevanagari(),
                lexeme.getLemmaSlp1(),
                lexeme.getGlossRu(),
                lexeme.getGlossEn(),
                lexeme.getLongDefinitionRu(),
                lexeme.getLongDefinitionEn(),
                lexeme.getGender(),
                lexeme.getPartsOfSpeech().stream().map(PartOfSpeech::getCode).toList(),
                lexeme.getMorphologyClasses().stream().map(MorphologyClass::getCode).toList(),
                lexeme.getSemanticTopics().stream().map(SemanticTopic::getId).toList(),
                wordForms,
                lexeme.getCreatedAt(),
                lexeme.getUpdatedAt());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}