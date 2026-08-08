package sm.selflearn.samskrtam.quiz.service;

import sm.selflearn.samskrtam.quiz.model.ItemType;

/**
 * Maps a curriculum quest-item type code to the {@link ItemType} used as the progress key
 * in {@code quiz_item_score}.
 *
 * <p>The legacy enum is deliberately NOT widened (decision 2026-08): every quest item is
 * keyed by {@code externalRefId = quest_item.id} under the item type of its domain.
 * Because quest_item ids and legacy ref ids (case_ending_id / vocabulary_word_id) live in
 * disjoint id spaces, ref-scoped progress queries never collide even though the enum
 * constant (e.g. DECLENSION_FORM) is shared by name.
 *
 * <ul>
 *   <li>MORPHOLOGY codes ({@code DECLENSION_FORM*}, {@code CASE_RECOGNITION},
 *       {@code DECLENSION_MATCH}) → {@link ItemType#DECLENSION_FORM}</li>
 *   <li>all other codes (future LEXICON etc.) → {@link ItemType#VOCABULARY_WORD}</li>
 * </ul>
 */
public final class QuestProgressTypes {

    private QuestProgressTypes() {
    }

    public static ItemType resolve(String questItemTypeCode) {
        if (questItemTypeCode == null) {
            return ItemType.VOCABULARY_WORD;
        }
        if (questItemTypeCode.startsWith("DECLENSION_") || questItemTypeCode.startsWith("CASE_")) {
            return ItemType.DECLENSION_FORM;
        }
        return ItemType.VOCABULARY_WORD;
    }
}