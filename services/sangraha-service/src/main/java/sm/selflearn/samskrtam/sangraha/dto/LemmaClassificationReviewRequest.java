package sm.selflearn.samskrtam.sangraha.dto;

/**
 * PATCH /sangraha/internal/lexicon/classifications/{id} (lemma-classification.md §4).
 * Позволяет исправить поля и сразу подтвердить/отклонить.
 */
public record LemmaClassificationReviewRequest(
        String status,
        String categoryCode,
        String glossRu,
        String glossEn,
        Short confidence) {
}