package sm.selflearn.samskrtam.sangraha.dto;

import java.util.UUID;

/**
 * Ответ запуска/статуса прогона (lemma-classification.md §3 шаги 6, 17).
 */
public record ClassificationRunResponse(
        UUID runId,
        String schemeCode,
        int requestedBatchCount,
        int completedBatchCount,
        String status,
        int succeededBatchCount,
        int failedBatchCount,
        int classifiedLemmaCount) {
}