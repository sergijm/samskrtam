package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexicalTopicBinding;
import sm.selflearn.samskrtam.curriculum.lexicon.model.LexicalTopicBindingId;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexicalTopicBindingRepository;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.model.TopicDomain;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;
import java.util.UUID;

/**
 * Привязка лексем к Lexical Topic (task-curriculum-16 §9, ADMIN): только для
 * Topic с domain == LEXICON, иначе 400.
 */
@Service
@RequiredArgsConstructor
public class LexicalTopicBindingService {

    private final TopicRepository topicRepository;
    private final LexemeRepository lexemeRepository;
    private final LexicalTopicBindingRepository bindingRepository;

    private Topic requireLexicalTopic(UUID topicId) {
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Topic not found"));
        if (topic.getDomain() != TopicDomain.LEXICON) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Topic domain must be LEXICON");
        }
        return topic;
    }

    @Transactional(readOnly = true)
    public List<UUID> listBindingLexemeIds(UUID topicId) {
        requireLexicalTopic(topicId);
        return bindingRepository.findByIdLexicalTopicId(topicId).stream()
                .map(b -> b.getId().getLexemeId())
                .toList();
    }

    /**
     * Полная идемпотентная замена набора лексем темы.
     */
    @Transactional
    public List<UUID> replaceBinding(UUID topicId, List<UUID> lexemeIds) {
        requireLexicalTopic(topicId);
        for (UUID lexemeId : lexemeIds) {
            addBinding(topicId, lexemeId);
        }
        List<UUID> existing = bindingRepository.findByIdLexicalTopicId(topicId).stream()
                .map(b -> b.getId().getLexemeId())
                .toList();
        for (UUID lexemeId : existing) {
            if (!lexemeIds.contains(lexemeId)) {
                removeBinding(topicId, lexemeId);
            }
        }
        return bindingRepository.findByIdLexicalTopicId(topicId).stream()
                .map(b -> b.getId().getLexemeId())
                .toList();
    }

    @Transactional
    public void addBinding(UUID topicId, UUID lexemeId) {
        requireLexicalTopic(topicId);
        Lexeme lexeme = lexemeRepository.findById(lexemeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lexeme not found"));
        if (bindingRepository.existsByIdLexicalTopicIdAndIdLexemeId(topicId, lexemeId)) {
            return;
        }
        Topic topic = topicRepository.findById(topicId).orElseThrow();
        LexicalTopicBinding binding = new LexicalTopicBinding();
        binding.setLexicalTopic(topic);
        binding.setLexeme(lexeme);
        LexicalTopicBindingId id = new LexicalTopicBindingId();
        id.setLexicalTopicId(topicId);
        id.setLexemeId(lexemeId);
        binding.setId(id);
        bindingRepository.save(binding);
    }

    @Transactional
    public void removeBinding(UUID topicId, UUID lexemeId) {
        requireLexicalTopic(topicId);
        LexicalTopicBindingId id = new LexicalTopicBindingId();
        id.setLexicalTopicId(topicId);
        id.setLexemeId(lexemeId);
        bindingRepository.findById(id).ifPresent(bindingRepository::delete);
    }
}