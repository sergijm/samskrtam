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
import sm.selflearn.samskrtam.curriculum.lexicon.dto.UserCollectionUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserCollection;
import sm.selflearn.samskrtam.curriculum.lexicon.service.UserCollectionService;

import java.util.List;
import java.util.UUID;

/**
 * Пользовательские коллекции (task-curriculum-16 §8): не ADMIN, свои + SHARED чужие.
 */
@RestController
@RequestMapping("/api/v2/lexicon/users/{userId}/collections")
@RequiredArgsConstructor
public class UserCollectionController {

    private final UserCollectionService collectionService;

    @GetMapping
    public List<UserCollection> list(@PathVariable UUID userId) {
        return collectionService.list(userId);
    }

    @GetMapping("/{collectionId}")
    public UserCollection get(@PathVariable UUID userId, @PathVariable UUID collectionId) {
        return collectionService.getAndCheck(collectionId, userId);
    }

    @PostMapping
    public UserCollection create(@PathVariable UUID userId,
                                 @RequestBody UserCollectionUpsertRequest request) {
        return collectionService.create(userId, request);
    }

    @PutMapping("/{collectionId}")
    public UserCollection update(@PathVariable UUID userId,
                                 @PathVariable UUID collectionId,
                                 @RequestBody UserCollectionUpsertRequest request) {
        return collectionService.update(userId, collectionId, request);
    }

    @DeleteMapping("/{collectionId}")
    public void delete(@PathVariable UUID userId, @PathVariable UUID collectionId) {
        collectionService.delete(userId, collectionId);
    }

    @PostMapping("/{collectionId}/items/{lexemeId}")
    public void addItem(@PathVariable UUID userId,
                        @PathVariable UUID collectionId,
                        @PathVariable UUID lexemeId) {
        collectionService.addItem(userId, collectionId, lexemeId);
    }

    @DeleteMapping("/{collectionId}/items/{lexemeId}")
    public void removeItem(@PathVariable UUID userId,
                           @PathVariable UUID collectionId,
                           @PathVariable UUID lexemeId) {
        collectionService.removeItem(userId, collectionId, lexemeId);
    }
}