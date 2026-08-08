package sm.selflearn.samskrtam.curriculum.lexicon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sm.selflearn.samskrtam.curriculum.lexicon.dto.UserCollectionUpsertRequest;
import sm.selflearn.samskrtam.curriculum.lexicon.model.CollectionItemAddedVia;
import sm.selflearn.samskrtam.curriculum.lexicon.model.CollectionVisibility;
import sm.selflearn.samskrtam.curriculum.lexicon.model.Lexeme;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserCollection;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserCollectionItem;
import sm.selflearn.samskrtam.curriculum.lexicon.model.UserCollectionItemId;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.LexemeRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserCollectionItemRepository;
import sm.selflearn.samskrtam.curriculum.lexicon.repository.UserCollectionRepository;

import java.util.List;
import java.util.UUID;

/**
 * Пользовательские коллекции (task-curriculum-16 §8): не ADMIN, свои + SHARED
 * чужие. ownerId берётся из пути, вызывающим выступает фронтенд/quiz-service.
 */
@Service
@RequiredArgsConstructor
public class UserCollectionService {

    private final UserCollectionRepository collectionRepository;
    private final UserCollectionItemRepository itemRepository;
    private final LexemeRepository lexemeRepository;

    @Transactional(readOnly = true)
    public List<UserCollection> list(UUID userId) {
        return collectionRepository.findByOwnerId(userId);
    }

    @Transactional(readOnly = true)
    public UserCollection getAndCheck(UUID collectionId, UUID userId) {
        UserCollection collection = collectionRepository.findById(collectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Collection not found"));
        if (!collection.getOwnerId().equals(userId)
                && collection.getVisibility() != CollectionVisibility.SHARED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return collection;
    }

    @Transactional
    public UserCollection create(UUID userId, UserCollectionUpsertRequest request) {
        UserCollection collection = new UserCollection();
        collection.setOwnerId(userId);
        apply(collection, request);
        return collectionRepository.save(collection);
    }

    @Transactional
    public UserCollection update(UUID userId, UUID collectionId, UserCollectionUpsertRequest request) {
        UserCollection collection = getAndCheck(collectionId, userId);
        if (!collection.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can edit");
        }
        apply(collection, request);
        return collectionRepository.save(collection);
    }

    @Transactional
    public void delete(UUID userId, UUID collectionId) {
        UserCollection collection = getAndCheck(collectionId, userId);
        if (!collection.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can delete");
        }
        collectionRepository.delete(collection);
    }

    @Transactional
    public void addItem(UUID userId, UUID collectionId, UUID lexemeId) {
        UserCollection collection = getAndCheck(collectionId, userId);
        ownerOnly(collection, userId);
        Lexeme lexeme = lexemeRepository.findById(lexemeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lexeme not found"));
        if (itemRepository.existsByIdCollectionIdAndIdLexemeId(collectionId, lexemeId)) {
            return;
        }
        UserCollectionItem item = new UserCollectionItem();
        item.setCollection(collection);
        item.setLexeme(lexeme);
        UserCollectionItemId id = new UserCollectionItemId();
        id.setCollectionId(collectionId);
        id.setLexemeId(lexemeId);
        item.setId(id);
        item.setAddedVia(CollectionItemAddedVia.MANUAL);
        itemRepository.save(item);
    }

    @Transactional
    public void removeItem(UUID userId, UUID collectionId, UUID lexemeId) {
        UserCollection collection = getAndCheck(collectionId, userId);
        ownerOnly(collection, userId);
        UserCollectionItemId itemId = new UserCollectionItemId();
        itemId.setCollectionId(collectionId);
        itemId.setLexemeId(lexemeId);
        itemRepository.findById(itemId).ifPresent(itemRepository::delete);
    }

    private void ownerOnly(UserCollection collection, UUID userId) {
        if (!collection.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only owner can modify");
        }
    }

    private void apply(UserCollection collection, UserCollectionUpsertRequest request) {
        collection.setName(request.name());
        collection.setDescription(request.description());
        if (request.visibility() != null) {
            collection.setVisibility(request.visibility());
        }
    }
}