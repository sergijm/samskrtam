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
import sm.selflearn.samskrtam.sangraha.model.LemmaClassification;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationBatchRepository;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationRunRepository;
import sm.selflearn.samskrtam.sangraha.repository.ClassificationSchemeRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaClassificationRepository;
import sm.selflearn.samskrtam.sangraha.repository.LemmaRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationLlmClient.LemmaClassificationCallException;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationPromptBuilder.LemmaBatchItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private final VerseWordRepository verseWordRepository;
    private final LemmaClassificationLlmClient llmClient;
    private final LemmaClassificationValidator validator;
    private final LlmProperties llmProperties;

    /**
     * Запускает прогон классификации.
     *
     * @param batchCount обязателен — явный ADMIN-лимит (§3 шаг 2), без дефолта
     * @throws IllegalArgumentException неактивная схема или batchCount <= 0 (400)
     */
    @Transactional
    public ClassificationRunResponse startRun(String schemeCode, Integer batchSize, Integer batchCount,
                                              String requestedBy) {
        int size = batchSize == null || batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
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
        log.info("Run candidates: scheme={}, selected={}", schemeCode, candidates.size());

        ClassificationRun run = ClassificationRun.builder()
                .schemeCode(schemeCode)
                .requestedBatchCount(batchCount)
                .completedBatchCount(0)
                .status(ClassificationRunStatus.RUNNING)
                .requestedBy(requestedBy)
                .build();
        run = runRepository.save(run);

        int succeeded = 0;
        int failed = 0;
        int classified = 0;

        for (int from = 0; from < candidates.size() && from / size < batchCount; from += size) {
            List<Lemma> batchLemmas = sublist(candidates, from, size);
            int batchIndex = from / size;
            ClassificationBatch batch = batchRepository.save(newClassificationBatch(run, batchIndex, batchLemmas, schemeCode));
            try {
                LemmaClassificationSuggestion.BatchResult result = llmClient.classifyBatch(toItems(batchLemmas));
                batch.setStatus(ClassificationBatchStatus.SUCCESS);
                batch.setLlmModel(result.llmModel());
                batch.setCompletedAt(Instant.now());
                batchRepository.save(batch);
                classified += upsertClassifications(batch, result);
                succeeded++;
            } catch (LemmaClassificationCallException e) {
                log.error("Batch {} failed for run {}", batch.getId(), run.getId(), e);
                batch.setStatus(ClassificationBatchStatus.FAILED);
                batch.setErrorMessage(e.getMessage());
                batch.setCompletedAt(Instant.now());
                batchRepository.save(batch);
                failed++;
            }
            run.setCompletedBatchCount(run.getCompletedBatchCount() + 1);
            runRepository.save(run);
        }

        run.setStatus(failed == 0 ? ClassificationRunStatus.COMPLETED : ClassificationRunStatus.COMPLETED_WITH_ERRORS);
        run.setCompletedAt(Instant.now());
        runRepository.save(run);

        log.info("Run {} done: status={}, succeeded={}, failed={}, classified={}",
                run.getId(), run.getStatus(), succeeded, failed, classified);
        return new ClassificationRunResponse(run.getId(), schemeCode, run.getRequestedBatchCount(),
                run.getCompletedBatchCount(), run.getStatus().name(), succeeded, failed, classified);
    }

    @Transactional(readOnly = true)
    public ClassificationRunResponse getRun(UUID runId) {
        ClassificationRun run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));
        List<ClassificationBatch> batches = batchRepository.findByRunId(runId);
        int succeeded = (int) batches.stream().filter(b -> b.getStatus() == ClassificationBatchStatus.SUCCESS).count();
        int failed = (int) batches.stream().filter(b -> b.getStatus() == ClassificationBatchStatus.FAILED).count();
        int classified = batches.stream().mapToInt(ClassificationBatch::getLemmaCount).sum();
        return new ClassificationRunResponse(run.getId(), run.getSchemeCode(), run.getRequestedBatchCount(),
                run.getCompletedBatchCount(), run.getStatus().name(), succeeded, failed, classified);
    }

    private ClassificationBatch newClassificationBatch(ClassificationRun run, int index, List<Lemma> lemmas,
                                                       String schemeCode) {
        return ClassificationBatch.builder()
                .schemeCode(schemeCode)
                .runId(run.getId())
                .batchIndex(index)
                .lemmaCount(lemmas.size())
                .status(ClassificationBatchStatus.PENDING)
                .llmModel(llmProperties.getModel())
                .build();
    }

    private List<LemmaBatchItem> toItems(List<Lemma> lemmas) {
        List<LemmaBatchItem> items = new ArrayList<>(lemmas.size());
        for (Lemma lemma : lemmas) {
            items.add(new LemmaBatchItem(lemma, examplesFor(lemma)));
        }
        return items;
    }

    private List<String> examplesFor(Lemma lemma) {
        List<VerseWord> words = verseWordRepository.findTop2ByLemmaIdOrderByPositionAsc(lemma.getId());
        List<String> examples = new ArrayList<>(words.size());
        for (VerseWord w : words) {
            examples.add(w.getSurfaceIast());
        }
        return examples;
    }

    private int upsertClassifications(ClassificationBatch batch,
                                      LemmaClassificationSuggestion.BatchResult result) {
        if (result.items() == null || result.items().isEmpty()) {
            return 0;
        }
        int saved = 0;
        for (LemmaClassificationSuggestion suggestion : result.items()) {
            if (validator.containsDevanagari(suggestion.glossRu()) || validator.containsDevanagari(suggestion.glossEn())) {
                log.warn("Devanagari in gloss, skipping lemma {}", suggestion.lemmaId());
                continue;
            }
            String categoryCode = validator.isValidCategoryCode(suggestion.categoryCode())
                    ? suggestion.categoryCode() : null;
            Lemma lemma = lemmaRepository.findById(suggestion.lemmaId()).orElse(null);
            if (lemma == null) {
                log.warn("Model returned unknown lemmaId {}, skipping", suggestion.lemmaId());
                continue;
            }
            LemmaClassification existing = classificationRepository
                    .findByLemmaIdAndSchemeCode(suggestion.lemmaId(), batch.getSchemeCode()).orElse(null);
            if (existing == null) {
                existing = LemmaClassification.builder()
                        .lemma(lemma)
                        .schemeCode(batch.getSchemeCode())
                        .status(ClassificationStatus.CANDIDATE)
                        .build();
            }
            existing.setCategoryCode(categoryCode);
            existing.setGlossRu(suggestion.glossRu());
            existing.setGlossEn(suggestion.glossEn());
            existing.setConfidence(suggestion.confidence());
            existing.setLlmModel(result.llmModel());
            existing.setBatchId(batch.getId());
            classificationRepository.save(existing);
            saved++;
        }
        return saved;
    }

    private static <T> List<T> sublist(List<T> list, int from, int size) {
        return list.subList(from, Math.min(from + size, list.size()));
    }
}