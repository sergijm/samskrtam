package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.model.Work;
import sm.selflearn.samskrtam.sangraha.model.Chapter;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.WorkRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static sm.selflearn.samskrtam.sangraha.service.VerseAnalysisSaver.getString;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerseAnalysisService {

    private final VerseRepository verseRepository;
    private final ChapterRepository chapterRepository;
    private final WorkRepository workRepository;
    private final LlmClient llmClient;
    private final VerseAnalysisSaver analysisSaver;
    private final ToolCallValidator toolCallValidator;
    private final JsonSchemas jsonSchemas;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    /**
     * Запускает анализ одного стиха LLM.
     * Сохраняет rawText в стих и делегирует в runAnalysis() с одним элементом.
     */
    public void analyze(UUID verseId, String rawText) {
        Verse verse = verseRepository.findByIdAndDeletedAtIsNull(verseId)
                .orElseThrow(() -> new IllegalArgumentException("Verse not found: " + verseId));

        if (rawText != null && !rawText.isBlank()) {
            verse.setRawText(rawText);
        }

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

        runAnalysis(versesToAnalyze);

        return versesToAnalyze.stream().map(Verse::getId).collect(Collectors.toList());
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
        // 1. Помечаем все ANALYZING
        Instant now = Instant.now();
        for (Verse v : verses) {
            v.setStatus(VerseStatus.ANALYZING);
            v.setUpdatedAt(now);
            verseRepository.save(v);
        }

        log.info("Starting batch analysis for {} verses (mode: {})", verses.size(),
                llmProperties.isTwoPass() ? "two-pass" : "single-pass");

        // 2. LLM-вызов
        JsonNode llmResponse;
        try {
            llmResponse = llmClient.call(verses);
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

        /*
         * 3. Извлекаем данные из LLM response.
         *
         * В зависимости от модели / gateway / tool-call parsing здесь реально
         * могут прийти разные формы:
         *
         *   [...]
         *
         *   {"verses":[...]}
         *
         *   {"verses":"{\"verses\":[...]}"}
         *
         *   "{\"verses\":[...]}"
         *
         *   {"arguments":"{\"verses\":[...]}"}
         *
         *   "```json\n{\"verses\":[...]}\n```"
         *
         * Поэтому здесь не доверяем типу JsonNode и нормализуем всё
         * до массива verses.
         */
        JsonNode versesArrayNode;

        try {
            versesArrayNode = llmClient.extractVersesArguments(llmResponse);

            if (log.isDebugEnabled()) {
                log.debug("Extracted LLM arguments: {}",
                        objectMapper.writeValueAsString(versesArrayNode));
            }

            /*
             * Максимальное количество итераций защиты от неожиданной
             * рекурсивной/циклической обёртки.
             */
            for (int depth = 0; depth < 10 && versesArrayNode != null; depth++) {

                if (versesArrayNode.isNull()) {
                    versesArrayNode = null;
                    break;
                }

                // Уже то, что нам нужно.
                if (versesArrayNode.isArray()) {
                    break;
                }

                /*
                 * Строка может содержать JSON:
                 *
                 * "{\"verses\":[...]}"
                 *
                 * или markdown:
                 *
                 * ```json
                 * {"verses":[...]}
                 * ```
                 */
                if (versesArrayNode.isTextual()) {
                    String text = versesArrayNode.asText();

                    if (text == null || text.isBlank()) {
                        versesArrayNode = null;
                        break;
                    }

                    text = text.trim();

                    // Убираем markdown code fence, если модель его добавила.
                    if (text.startsWith("```")) {
                        int firstNewline = text.indexOf('\n');
                        int lastFence = text.lastIndexOf("```");

                        if (firstNewline >= 0 && lastFence > firstNewline) {
                            text = text.substring(firstNewline + 1, lastFence).trim();
                        }
                    }

                    try {
                        versesArrayNode = objectMapper.readTree(text);
                        continue;
                    } catch (Exception e) {
                        log.warn("LLM returned textual value which is not valid JSON: {}",
                                text.length() > 500
                                        ? text.substring(0, 500) + "..."
                                        : text);
                        versesArrayNode = null;
                        break;
                    }
                }

                /*
                 * Объект.
                 *
                 * Ищем стандартные обёртки:
                 *
                 * {"verses": ...}
                 * {"arguments": ...}
                 * {"parameters": ...}
                 * {"result": ...}
                 * {"data": ...}
                 */
                if (versesArrayNode.isObject()) {

                    // Главный ожидаемый случай.
                    JsonNode nestedVerses = versesArrayNode.get("verses");
                    if (nestedVerses != null && !nestedVerses.isNull()) {
                        versesArrayNode = nestedVerses;
                        continue;
                    }

                    // Иногда tool arguments дополнительно обёрнуты.
                    JsonNode arguments = versesArrayNode.get("arguments");
                    if (arguments != null && !arguments.isNull()) {
                        versesArrayNode = arguments;
                        continue;
                    }

                    JsonNode parameters = versesArrayNode.get("parameters");
                    if (parameters != null && !parameters.isNull()) {
                        versesArrayNode = parameters;
                        continue;
                    }

                    JsonNode result = versesArrayNode.get("result");
                    if (result != null && !result.isNull()) {
                        versesArrayNode = result;
                        continue;
                    }

                    JsonNode data = versesArrayNode.get("data");
                    if (data != null && !data.isNull()) {
                        versesArrayNode = data;
                        continue;
                    }

                    /*
                     * Если это сам объект одного стиха:
                     *
                     * {
                     *   "verseIndex": 0,
                     *   "textDevanagari": "...",
                     *   ...
                     * }
                     *
                     * превращаем его в массив из одного элемента.
                     */
                    if (versesArrayNode.has("verseIndex")) {
                        ArrayNode singleVerseArray = objectMapper.createArrayNode();
                        singleVerseArray.add(versesArrayNode);
                        versesArrayNode = singleVerseArray;
                        break;
                    }

                    log.warn("LLM returned JSON object, but no verses/arguments/result/data "
                                    + "field was found. Keys: {}",
                            java.util.stream.StreamSupport.stream(
                                            java.util.Spliterators.spliteratorUnknownSize(
                                                    versesArrayNode.fieldNames(), 0),
                                            false)
                                    .toList());

                    versesArrayNode = null;
                    break;
                }

                // Любой другой JSON type нам не подходит.
                log.warn("Unsupported LLM response JSON node type: {}",
                        versesArrayNode.getNodeType());

                versesArrayNode = null;
                break;
            }

        } catch (Exception e) {
            log.error("Failed to normalize LLM response for batch of {} verses",
                    verses.size(), e);
            versesArrayNode = null;
        }

        if (versesArrayNode == null || !versesArrayNode.isArray()) {
            log.error(
                    "LLM did not return a usable verses array. Extracted node: {}",
                    versesArrayNode == null
                            ? "null"
                            : versesArrayNode.toString()
            );

            for (Verse v : verses) {
                analysisSaver.markFailed(v);
            }
            return;
        }

        if (log.isDebugEnabled()) {
            try {
                log.debug("Normalized verses array: {}",
                        objectMapper.writeValueAsString(versesArrayNode));
            } catch (JsonProcessingException ignored) {
            }
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
                        analyzerName
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


    private void saveSingleVerseResult(Verse verse, JsonNode verseEntry,
                                        String rawResponse, String modelName, String analyzerName) {
        Chapter chapter = chapterRepository.findByIdAndDeletedAtIsNull(verse.getChapterId())
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + verse.getChapterId()));
        Work work = workRepository.findById(chapter.getWorkId())
                .orElseThrow(() -> new IllegalArgumentException("Work not found: " + chapter.getWorkId()));

        String textDevanagari = getString(verseEntry, "textDevanagari");
        String textIast = getString(verseEntry, "textIast");
        String translationRu = getString(verseEntry, "translationRu");
        String translationEn = getString(verseEntry, "translationEn");
        JsonNode sandhiSplitsNode = verseEntry.get("sandhiSplits");
        JsonNode wordsNode = verseEntry.get("words");

        if (textDevanagari == null || textIast == null || translationRu == null || translationEn == null
                || sandhiSplitsNode == null || !sandhiSplitsNode.isArray()
                || wordsNode == null || !wordsNode.isArray()) {
            log.error("Invalid tool call arguments for verse {}: missing required fields", verse.getId());
            analysisSaver.markFailed(verse);
            return;
        }

        try {
            analysisSaver.saveResults(verse, work, chapter,
                    textDevanagari, textIast, translationRu, translationEn,
                    sandhiSplitsNode, wordsNode, rawResponse, modelName, analyzerName);
        } catch (Exception e) {
            log.error("Failed to save analysis results for verse {}, reverting to DRAFT", verse.getId(), e);
            analysisSaver.revertToDraft(verse);
            throw e;
        }
    }
}

