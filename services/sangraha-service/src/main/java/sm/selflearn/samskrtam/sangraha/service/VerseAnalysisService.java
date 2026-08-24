package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static sm.selflearn.samskrtam.sangraha.service.VerseAnalysisSaver.getString;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerseAnalysisService {

    /**
     * Максимальный размер чанка для analyzeVerses (batch-verse-review.md):
     * {@code max-completion-tokens / 3000} (3000 токенов — ориентир на один стих
     * с полным разбором), но не больше {@link #ANALYSIS_CHUNK_SIZE_MAX}.
     * Чтобы не увеличивать LLM-промпт сверх проверенного на практике.
     */
    private static final int ANALYSIS_CHUNK_SIZE_MAX = 3;
    private static final int ANALYSIS_CHUNK_SIZE_DEFAULT = 3;
    private static final int ANALYSIS_TOKENS_PER_VERSE = 3000;

    private final VerseRepository verseRepository;
    private final ChapterRepository chapterRepository;
    private final WorkRepository workRepository;
    private final VerseWordRepository verseWordRepository;
    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    private final VerseAnalysisSaver analysisSaver;
    private final VerseAnalysisResponseNormalizer responseNormalizer;
    private final ToolCallValidator toolCallValidator;
    private final JsonSchemas jsonSchemas;
    private final VerseBatchPushService verseBatchPushService;
    private final TransliterationService transliterationService;

    /**
     * Размер батча для анализа стихов: {@code max-completion-tokens / 3000},
     * но не больше {@link #ANALYSIS_CHUNK_SIZE_MAX}. Если max-completion-tokens
     * не задан — {@link #ANALYSIS_CHUNK_SIZE_DEFAULT}.
     */
    private int analysisChunkSize() {
        Integer maxTokens = llmProperties.getMaxCompletionTokens();
        if (maxTokens == null || maxTokens <= 0) {
            return ANALYSIS_CHUNK_SIZE_DEFAULT;
        }
        int computed = maxTokens / ANALYSIS_TOKENS_PER_VERSE;
        if (computed < 1) {
            computed = 1;
        }
        return Math.min(computed, ANALYSIS_CHUNK_SIZE_MAX);
    }

    /**
     * Запускает анализ одного стиха LLM.
     * Поле rawText (исходный текст) изменяется только со стороны фронтэнда вручную —
     * здесь оно НЕ перезаписывается. Текстовые колонки text_iast/text_devanagari
     * нормализуются из rawText на этапе runAnalysis.
     */
    public void analyze(UUID verseId, String rawText) {
        Verse verse = verseRepository.findByIdAndDeletedAtIsNull(verseId)
                .orElseThrow(() -> new IllegalArgumentException("Verse not found: " + verseId));

        runAnalysis(List.of(verse));
    }

    /**
     * Запускает анализ всех стихов главы со статусом DRAFT/FAILED.
     * Если подходящих стихов нет — бросает IllegalStateException (контроллер вернёт 409).
     *
     * @return список verseId, отправленных на анализ
     */
    public List<UUID> analyzeChapter(UUID chapterId) {
        Chapter chapter = chapterRepository.findByIdAndDeletedAtIsNull(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + chapterId));

        List<Verse> allVerses = verseRepository
                .findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(chapterId);

        List<Verse> versesToAnalyze = allVerses.stream()
                .filter(v -> v.getStatus() == VerseStatus.DRAFT || v.getStatus() == VerseStatus.FAILED)
                .collect(Collectors.toList());

        if (versesToAnalyze.isEmpty()) {
            throw new IllegalStateException("No verses to analyze in chapter " + chapterId
                    + " (all verses have status different from DRAFT/FAILED)");
        }

        int chunkSize = analysisChunkSize();
        for (int i = 0; i < versesToAnalyze.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, versesToAnalyze.size());
            runAnalysis(versesToAnalyze.subList(i, end));
        }

        return versesToAnalyze.stream().map(Verse::getId).collect(Collectors.toList());
    }

    /**
     * Батч-анализ произвольного списка стихов (sangraha-service/batch-verse-review.md,
     * POST /api/v1/sangraha/verse/analysis).
     * В отличие от {@link #analyzeChapter} — не фильтрует по статусу: все переданные
     * стихи анализируются безусловно, включая уже ANALYZED (полная перезапись анализа).
     * Не найденные/удалённые id пропускаются молча. Чанки обрабатываются последовательно.
     *
     * @return список id реально загруженных стихов (в порядке запроса), принятых к анализу
     */
    public List<UUID> analyzeVerses(List<UUID> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            return List.of();
        }

        List<Verse> found = verseRepository.findAllByIdInAndDeletedAtIsNull(verseIds);
        Map<UUID, Verse> verseById = new HashMap<>();
        for (Verse verse : found) {
            verseById.put(verse.getId(), verse);
        }

        List<Verse> ordered = new ArrayList<>();
        for (UUID id : verseIds) {
            Verse verse = verseById.get(id);
            if (verse != null) {
                ordered.add(verse);
            }
        }

        int chunkSize = analysisChunkSize();
        for (int i = 0; i < ordered.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, ordered.size());
            runAnalysis(ordered.subList(i, end));
        }

        return ordered.stream().map(Verse::getId).collect(Collectors.toList());
    }

    /**
     * Общий метод анализа списка стихов.
     * <ol>
     *   <li>Выставляет всем стихам статус ANALYZING</li>
     *   <li>Вызывает LLM с batch-промптом</li>
     *   <li>Извлекает массив verses[] из ответа</li>
     *   <li>По verseIndex сопоставляет результаты со входными стихами</li>
     *   <li>Для каждого — try/catch saveResults; ошибка по одному стиху не прерывает остальные</li>
     *   <li>Стихи без результата — markFailed</li>
     * </ol>
     */
    private void runAnalysis(List<Verse> verses) {
        // 1. Помечаем все ANALYZING и нормализуем текстовые колонки из rawText
        //    (детектируем iast|devanagari и заполняем text_iast/text_devanagari).
        //    Само поле rawText не трогаем — оно меняется только вручную с фронтэнда.
        Instant now = Instant.now();
        for (Verse v : verses) {
            prepareVerseForAnalysis(v, now);
        }

        log.info("Starting batch analysis for {} verses", verses.size());

        // 2. LLM-вызов
        JsonNode llmResponse;
        String rawPrompt;
        try {
            var result = llmClient.callWithResult(verses);
            llmResponse = result == null ? null : result.response();
            rawPrompt = result == null ? null : result.rawPrompt();
        } catch (Exception e) {
            log.error("LLM batch analysis failed for {} verses", verses.size(), e);
            for (Verse v : verses) {
                analysisSaver.markFailed(v);
            }
            return;
        }

        if (llmResponse == null || llmResponse.isNull()) {
            log.error("LLM returned null response for batch of {} verses", verses.size());
            for (Verse v : verses) {
                analysisSaver.markFailed(v);
            }
            return;
        }

        // 3. Извлекаем и нормализуем массив verses из ответа LLM.
        JsonNode versesArrayNode = responseNormalizer.normalizeToVersesArray(llmResponse);

        if (versesArrayNode == null || !versesArrayNode.isArray()) {
            log.error("LLM did not return a usable verses array for batch of {} verses",
                    verses.size());
            for (Verse v : verses) {
                analysisSaver.markFailed(v);
            }
            return;
        }

        String modelName = llmClient.extractModelName(llmResponse);
        String analyzerName = modelName;

        // 4. Индексируем входные стихи по позиции для сопоставления verseIndex
        Map<Integer, Verse> verseByIndex = new java.util.HashMap<>();
        for (int i = 0; i < verses.size(); i++) {
            verseByIndex.put(i, verses.get(i));
        }

        // 5. Обрабатываем каждый элемент verses[]
        List<Integer> processedIndices = new ArrayList<>();

        for (JsonNode verseEntry : versesArrayNode) {
            if (verseEntry == null || !verseEntry.isObject()) {
                log.warn("Unexpected verse entry type in LLM response: {}",
                        verseEntry == null ? "null" : verseEntry.getNodeType());
                continue;
            }

            int verseIndex = verseEntry.path("verseIndex").asInt(-1);

            if (verseIndex < 0 || !verseByIndex.containsKey(verseIndex)) {
                log.warn("Unexpected verseIndex {} in LLM response, skipping",
                        verseIndex);
                continue;
            }

            Verse verse = verseByIndex.get(verseIndex);
            processedIndices.add(verseIndex);

            try {
                saveSingleVerseResult(
                        verse,
                        verseEntry,
                        llmResponse.toString(),
                        modelName,
                        analyzerName,
                        rawPrompt
                );
            } catch (Exception e) {
                log.error(
                        "Failed to save analysis for verse {} (verseIndex={}), marking as FAILED",
                        verse.getId(),
                        verseIndex,
                        e
                );
                analysisSaver.markFailed(verse);
            }
        }

        // 6. Стихи, для которых не пришло результата — markFailed
        for (int i = 0; i < verses.size(); i++) {
            if (!processedIndices.contains(i)) {
                Verse verse = verses.get(i);

                log.error(
                        "No result in LLM response for verse {} (verseIndex={})",
                        verse.getId(),
                        i
                );

                analysisSaver.markFailed(verse);
            }
        }

        log.info(
                "Batch analysis completed: {} verses processed out of {}",
                processedIndices.size(),
                verses.size()
        );
    }


    /**
     * Подготавливает стих к анализу: выставляет статус ANALYZING и заполняет
     * текстовые колонки (text_iast / text_devanagari) на основе исходного rawText.
     * Письменность определяется по rawText:
     * <ul>
     *   <li>деванагари → textDevanagari = rawText, textIast = devanagariToIast(rawText);</li>
     *   <li>иначе (IAST) → textIast = rawText, textDevanagari = iastToDevanagari(rawText).</li>
     * </ul>
     * Поле rawText остаётся нетронутым.
     */
    private void prepareVerseForAnalysis(Verse verse, Instant now) {
        verse.setStatus(VerseStatus.ANALYZING);
        verse.setUpdatedAt(now);

        String raw = verse.getRawText();
        if (raw != null && !raw.isBlank()) {
            if ("devanagari".equals(transliterationService.detectScript(raw))) {
                verse.setTextDevanagari(raw);
                verse.setTextIast(transliterationService.devanagariToIast(raw));
            } else {
                verse.setTextIast(raw);
                verse.setTextDevanagari(transliterationService.iastToDevanagari(raw));
            }
        }

        verseRepository.save(verse);
    }

    private void saveSingleVerseResult(Verse verse, JsonNode verseEntry,
                                        String rawResponse, String modelName, String analyzerName,
                                        String rawPrompt) {
        // Standalone-стихи (страница /analysis) не привязаны к главе/произведению —
        // контекст work/chapter для них отсутствует и в saveResults передаётся null.
        final Chapter chapter;
        final Work work;
        if (verse.getChapterId() != null) {
            chapter = chapterRepository.findByIdAndDeletedAtIsNull(verse.getChapterId())
                    .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + verse.getChapterId()));
            work = workRepository.findById(chapter.getWorkId())
                    .orElseThrow(() -> new IllegalArgumentException("Work not found: " + chapter.getWorkId()));
        } else {
            chapter = null;
            work = null;
        }

        String textIast = getString(verseEntry, "textIast");
        String translationRu = getString(verseEntry, "translationRu");
        String translationEn = getString(verseEntry, "translationEn");
        JsonNode sandhiSplitsNode = verseEntry.get("sandhiSplits");
        JsonNode wordsNode = verseEntry.get("words");

        // textIast из ответа LLM НЕ сохраняется в колонку — остаётся только внутри
        // raw_model_response (JSON). Текстовые колонки text_iast/text_devanagari уже
        // заполнены из rawText на этапе prepareVerseForAnalysis. Поэтому здесь
        // достаточно проверить наличие textIast как признак корректного ответа.
        if (textIast == null || translationRu == null || translationEn == null
                || sandhiSplitsNode == null || !sandhiSplitsNode.isArray()
                || wordsNode == null || !wordsNode.isArray()) {
            log.error("Invalid tool call arguments for verse {}: missing required fields", verse.getId());
            analysisSaver.markFailed(verse);
            return;
        }

        try {
            analysisSaver.saveResults(verse, work, chapter,
                    translationRu, translationEn,
                    sandhiSplitsNode, wordsNode, rawResponse, modelName, analyzerName, rawPrompt);
            // Инкрементальная пачка лемм в curriculum-service (lexicon-content-pipeline.md §7).
            // Вне транзакции: сбой curriculum-service не откатывает анализ (см. VerseBatchPushService).
            verseBatchPushService.push(verse, work, chapter,
                    verseWordRepository.findAllByVerse_IdOrderByPositionAsc(verse.getId()));
        } catch (Exception e) {
            log.error("Failed to save analysis results for verse {}, reverting to DRAFT", verse.getId(), e);
            analysisSaver.revertToDraft(verse);
            throw e;
        }
    }
}

