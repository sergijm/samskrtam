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
import sm.selflearn.samskrtam.curriculum.lexicon.service.LexicalTopicBindingService;

import java.util.List;
import java.util.UUID;

/**
 * Привязка лексем к Lexical Topic (task-curriculum-16 §9, ADMIN).
 */
@RestController
@RequestMapping("/api/v2/lexicon/topics")
@RequiredArgsConstructor
public class LexicalTopicController {

    private final LexicalTopicBindingService bindingService;

    @GetMapping("/{topicId}/binding")
    public List<UUID> listBindingLexemeIds(@PathVariable UUID topicId) {
        return bindingService.listBindingLexemeIds(topicId);
    }

    @PutMapping("/{topicId}/binding")
    public List<UUID> replaceBinding(@PathVariable UUID topicId,
                                     @RequestBody List<UUID> lexemeIds) {
        return bindingService.replaceBinding(topicId, lexemeIds);
    }

    @PostMapping("/{topicId}/binding/{lexemeId}")
    public void addBinding(@PathVariable UUID topicId, @PathVariable UUID lexemeId) {
        bindingService.addBinding(topicId, lexemeId);
    }

    @DeleteMapping("/{topicId}/binding/{lexemeId}")
    public void removeBinding(@PathVariable UUID topicId, @PathVariable UUID lexemeId) {
        bindingService.removeBinding(topicId, lexemeId);
    }
}