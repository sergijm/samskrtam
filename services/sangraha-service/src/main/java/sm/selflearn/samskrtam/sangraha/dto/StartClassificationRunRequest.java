package sm.selflearn.samskrtam.sangraha.dto;

/**
 * Запрос запуска классификации (lemma-classification.md §3,
 * task-sangraha-18 шаг 16). batchCount обязателен — без дефолта
 * (явный ADMIN-лимит на объём прогона).
 */
public record StartClassificationRunRequest(
        String schemeCode,
        Integer batchSize,
        Integer batchCount) {

    public StartClassificationRunRequest {
        batchSize = batchSize == null ? 50 : batchSize;
    }
}