package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.sangraha.dto.ClassificationRunResponse;
import sm.selflearn.samskrtam.sangraha.model.ClassificationBatch;
import sm.selflearn.samskrtam.sangraha.model.ClassificationBatchStatus;
import sm.selflearn.samskrtam.sangraha.model.ClassificationRun;
import sm.selflearn.samskrtam.sangraha.model.ClassificationRunStatus;
import sm.selflearn.samskrtam.sangraha.model.ClassificationScheme;
import sm.selflearn.samskrtam.sangraha.model.ClassificationStatus;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sanggraha.model.LemmaClassification;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationBatchRepository;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationRunRepository;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationSchemeRepository;
import sm.selflearn.samskrtam.sangraha.repository.CurriculumSemanticTopicRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaClassificationRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationLlmClient.LemmaClassificationCallException;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationPromptBuilder.LemmaBatchItem;
import sm.selflearn.samskrtam.sangraha.model.ClassificationBatchStatus;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Оркестрация прогона batch-классификации (lemma-classification.md §3,
 * task-sangraha-18 часть D).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LemmaClassificationRunService {

    private static final int DEFAULT_BATCH_SIZE = 50;

    private final LemmaRepository lemmaRepository;
    private final ClassificationRunRepository runRepository;
    private final ClassificationBatchRepository batchRepository;
    private final ClassificationSchemeRepository schemeRepository;
    private final LemmaClassificationRepository classificationRepository;
    private final CurriculumSemanticTopicRepository topicRepository;
    private final VerseWordRepository verseWordRepository;
    private final LemmaClassificationLlmClient llmClient;
    private final LemmaClassificationValidator validator;
    private final LemmaClassificationPromptBuilder promptBuilder;

    /**
     * Запуск прогона (ADMIN). @param requestedBy — X-User-Id администратора.
     *
     * @throws IllegalArgumentException неактивная схема (400)
     */
    @Transactional
    public ClassificationRunResponse startRun(String schemeCode, Integer batchSize, Integer batchCount,
                                              String requestedBy) {
        int size = batchSize == null ? DEFAULT_BATCH_SIZE : batchSize;
        if (batchCount == null || batchCount <= 0) {
            throw new IllegalArgumentException("batchCount is required and must be positive");
        }
        ClassificationScheme scheme = schemeRepository.findById(schemeCode)
                .orElseThrow(() -> new IllegalArgumentException("Unknown schemeCode: " + schemeCode));
        if (!scheme.isActive()) {
            throw new IllegalArgumentException("Classification scheme is not active: " + schemeCode);
        }

        List<Lemma> candidates = lemmaRepository.findCandidatesForClassification(
                schemeCode, ClassificationStatus.REJECTED, PageRequest.of(0, size * batchCount));

        ClassificationRun run = ClassificationRun.builder()
                .schemeCode(schemeCode)
                .requestedBatchCount(batchCount)
                .completedBatchCount(0)
                .status(ClassificationRunStatus.RUNNING)
                .requestedBy(requestedBy)
                .build();
        run = runRepository.save(run);

        log.info("Classification run {} started: scheme={}, batches={}x{}={}, candidates={}",
                run.getId(), schemeCode, batchCount, size, size * batchCount, candidates.size());

        int succeeded = 0;
        int failed = 0;
        int classified = 0;

        for (int from = 0, idx = 0; from < candidates.size(); from += size, idx++) {
            if (idx >= batchCount) {
                break;
            }
            List<Lemma> batchLemmas = sublist(candidates, from, size);
            ClassificationBatch batch = batchRepository.save(startBatch(run, idx, batchLemmas, schemeCode));
            int batchIndex = idx;
            try {
                List<LemmaBatchItem> items = batchItems(batchLemmas);
                LemmaClassificationSuggestion.BatchResult result = llmClient.classifyBatch(items);
                batch.setStatus(ClassificationBatchStatus.SUCCESS);
                batch.setLlmModel(result.llmModel());
                batch.setCompletedAt(Instant.now());
                batchRepository.save(batch);
                batchIndex=from;
                classified += upsertClassifications(run, batch.getId(), schemeCode, result);
                succeeded++;
            } catch (LemmaClassificationCallException e) {
                log.error("Batch [{}] failed for run {}", batch.getId(), run.getId(), e);
                batch.setStatus(ClassificationBatchStatus.FAILED);
                batch.setErrorMessage(e.getMessage());
                batch.setCompletedAt(Instant.now());
                batchRepository.save(batch);
                failed++;
            }
            run.setCompletedBatchCount(run.getCompletedBatchCount() + 1);
            runRepository.save(run);
        }

        boolean allDone = run.getCompletedBatchCount() >= run.getRequestedBatchCount();
        ClassificationRunStatus finalStatus = (succeeded > 0 && failed == 0 && allDone)
                ? ClassificationRunStatus.COMPLETED
                : (failed > 0 ? ClassificationRunStatus.COMPLETED_WITH_ERRORS : ClassificationRunStatus.COMPLETED);
        run.setStatus(finalStatus);
        run.setCompletedAt(Instant.now());
        runRepository.save(run);

        log.info("Run {} finished: status={}, succeeded={}, failed={}, classified={}",
                run.getId(), finalStatus, succeeded, failed, classified);
        return new ClassificationRunResponse(run.getId(), schemeCode, run.getRequestedBatchCount(),
                run.getCompletedBatchCount(), finalStatus.name(), succeeded, failed, classified);
    }

    @Transactional(readOnly = true)
    public ClassificationRunResponse getRun(UUID runId) {
        ClassificationRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        List<ClassificationBatch> batches = batchRepository.findByRunId(runId);
        int succeeded = (int) batches.stream()
                .filter(b -> b.getStatus() == ClassificationBatchStatus.SUCCESS).count();
        int failed = (int) batches.stream()
                .filter(b -> b.getStatus() == ClassificationBatchStatus.FAILED).count();
        int classified = (int) classificationRepository.findByBatchIdIn(batches.stream().map(ClassificationBatch::getId).toList())
                .size();
        return new ClassificationRunResponse(run.getId(), run.getSchemeCode(), run.getRequestedBatchCount(),
                run.getCompletedBatchCount(), run.getStatus().name(), succeeded, failed, classified);
    }

    private ClassificationBatch startBatch(ClassificationRun run, int index, List<Lemma> lemmas, String schemeCode) {
        return ClassificationBatch.builder()
                .schemeCode(schemeCode)
                .runId(run.getId())
                .batchIndex(index)
                .lemmaCount(lemmas.size())
                .status(ClassificationBatchStatus.PENDING)
                .llmModel(llmClientmodel())
                .build();
    }

    private String llmModel() {
        return llmPropertiesModel();
    }

    private int upsertClassifications(LemmaClassificationSuggestion.BatchResult result, UUID batchId,
                                      String schemeCode) {
        if (result.items() == null) {
            return 0;
        }
        int saved = 0;
        for (LemmaClassificationSuggestion suggestion : result.items()) {
            boolean glossesOk = !validator.containsDevanagari(suggestion.glossRu())
                    && !validator.containsDevanagari(suggestion.glossEn());
            if (!glossesOk) {
                log.warn("Rejecting lemma {} by devanagari-in-gloss rule", suggestion.lemmaId());
                continue;
            }
            String categoryCode = validator.isValidCategoryCode(suggestion.categoryCode())
                    ? suggestion.categoryCode() : null;
            Lemma lemma = lemmaRepository.findById(suggestion.lemmaId()).orElse(null);
            if (lemma == null) {
                log.warn("Skipping unknown lemmaId from model: {}", suggestion.lemmaId());
                continue;
            }
            LemmaClassification existing = classificationRepository
                    .findByLemmaIdAndSchemeCode(suggestion.lemmaId(), schemeCode).orElse(null);
            if (existing == null) {
                existing = LemmaClassification.builder()
                        .lemma(lemma)
                        .schemeCode(schemeCode)
                        .status(ClassificationStatus.CANDIDATE)
                        .build();
            }
            existing.setCategoryCode(categoryCode);
            existing.setGlossRu(suggestion.glossRu());
            existing.setGlossEn(suggestion.glossEn());
            existing.setConfidence(suggestion.confidence());
            existing.setLlmModel(result.llmModel());
            existing.setBatchId(batchId);
            classificationRepository.save(existing);
            saved++;
        }
        return saved;
    }

    private static <T> List<T> sublist(List<T> list, int from, int size) {
        return list.subList(from, Math.min(from + size, list.size()));
    }
}