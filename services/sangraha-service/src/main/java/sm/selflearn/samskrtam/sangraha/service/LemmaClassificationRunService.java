package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.sangraha.dto.ClassificationRunResponse;
import sm.selflearn.samskrtam.sangraha.model.ClassificationScheme;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationSchemeRepository;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationLlmClient.LemmaClassificationCallException;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationPromptBuilder.LemmaBatchItem;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaClassificationRunService {

    private static final int DEFAULT_BATCH_SIZE = 50;

    private final ClassificationSchemeRepository schemeRepository;
    private final LemmaClassificationLlmClient llmClient;
    private final ClassificationResultPersister persister;
    private final ClassificationCandidateReader candidateReader;
    private final LlmProperties llmProperties;
    private final LlmConfigRegistry llmConfigRegistry;

    public ClassificationRunResponse startRun(String schemeCode, Integer batchSize, Integer batchCount,
                                              String llmModel, String requestedBy) {
        int size = batchSize == null || batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
        if (batchCount == null || batchCount <= 0) {
            throw new IllegalArgumentException("batchCount is required and must be positive");
        }
        ClassificationScheme scheme = schemeRepository.findById(schemeCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown schemeCode: " + schemeCode));
        if (!scheme.isActive()) {
            throw new IllegalArgumentException("Classification scheme is not active: " + schemeCode);
        }

        String resolvedModel = llmModel != null && !llmModel.isBlank() ? llmModel : llmProperties.getModel();
        LlmConfig config = llmModel != null && !llmModel.isBlank()
                ? llmConfigRegistry.resolveFor(llmModel).orElse(null) : null;
        if (llmModel != null && !llmModel.isBlank() && config == null) {
            throw new IllegalArgumentException("Model not found in llm.yaml: " + llmModel);
        }
        log.info("Classification run: model={}, batchSize={}, batchCount={}", resolvedModel, size, batchCount);

        List<LemmaBatchItem> items = candidateReader.loadCandidates(schemeCode, size, batchCount);
        log.info("Candidates loaded: {}", items.size());

        int succeeded = 0;
        int failed = 0;
        int classified = 0;

        for (int from = 0; from < items.size(); from += size) {
            List<LemmaBatchItem> batch = sublist(items, from, size);
            int batchIndex = from / size;
            try {
                LemmaClassificationSuggestion.BatchResult result = llmClient.classifyBatch(batch, config, resolvedModel);
                classified += persister.persist(result.items(), batch, schemeCode, resolvedModel);
                succeeded++;
            } catch (LemmaClassificationCallException e) {
                log.error("Batch {} failed: {}", batchIndex, e.getMessage());
                failed++;
            }
        }

        log.info("Classification done: succeeded={}, failed={}, classified={}", succeeded, failed, classified);
        return new ClassificationRunResponse(succeeded, failed, classified);
    }

    private static <T> List<T> sublist(List<T> list, int from, int size) {
        int to = Math.min(from + size, list.size());
        return from >= to ? List.of() : list.subList(from, to);
    }
}