package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import sm.selflearn.samskrtam.curriculum.lexicon.model.CollectionVisibility;

/**
 * Создание/обновление пользовательской коллекции (task-curriculum-16 §8).
 */
public record UserCollectionUpsertRequest(
        String name,
        String description,
        CollectionVisibility visibility
) {
}