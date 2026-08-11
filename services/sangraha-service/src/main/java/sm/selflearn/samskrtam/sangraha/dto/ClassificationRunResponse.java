package sm.selflearn.samskrtam.sangraha.dto;

public record ClassificationRunResponse(
        int succeededBatchCount,
        int failedBatchCount,
        int classifiedLemmaCount) {
}