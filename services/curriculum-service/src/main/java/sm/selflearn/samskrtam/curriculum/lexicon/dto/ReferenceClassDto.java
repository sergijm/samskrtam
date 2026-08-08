package sm.selflearn.samskrtam.curriculum.lexicon.dto;

import sm.selflearn.samskrtam.curriculum.lexicon.model.MorphologyAppliesTo;
import sm.selflearn.samskrtam.curriculum.lexicon.model.PosGroup;

/**
 * Справочная запись PartOfSpeech / MorphologyClass (task-curriculum-16 §6).
 */
public record ReferenceClassDto(
        String code,
        String group,
        MorphologyAppliesTo appliesTo,
        String nameRu,
        String nameEn
) {
    public static ReferenceClassDto pos(String code, PosGroup group, String ru, String en) {
        return new ReferenceClassDto(code, group.name(), null, ru, en);
    }

    public static ReferenceClassDto morphology(String code, MorphologyAppliesTo appliesTo, String ru, String en) {
        return new ReferenceClassDto(code, null, appliesTo, ru, en);
    }
}