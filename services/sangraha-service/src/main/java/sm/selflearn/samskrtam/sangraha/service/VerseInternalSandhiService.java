package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.model.VerseStatus;
import sm.selflearn.samskrtam.sangraha.repository.ChapterRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * ШАГ 2 анализа стиха: внутренние сандхи / словообразование (tool submit_word_formations).
 * Запускается ТОЛЬКО по явному запросу (эндпоинты VerseInternalSandhiController) —
 * не стартует автоматически после шага 1. Потребляет слова (VerseWord), сохранённые
 * шагом 1, и дописывает formationRuleNumbers (маркер «требуется ШАГ 2» снимается).
 * Статус стиха (ANALYZED) не меняется.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerseInternalSandhiService {

    private static final int CHUNK_SIZE_MAX_DEFAULT = 3;
    private static final int CHUNK_SIZE_DEFAULT_FALLBACK = 3;
    private static final int TOKENS_PER_VERSE_FALLBACK = 3000;

    private final VerseRepository verseRepository;
    private final ChapterRepository chapterRepository;
    private final VerseWordRepository verseWordRepository;
    private final LlmClient llmClient;
    private final JsonSchemas jsonSchemas;
    private final ToolCallValidator toolCallValidator;
    private final VerseAnalysisSaver analysisSaver;
    private final LlmProperties llmProperties;
    private final LlmConfigRegistry llmConfigRegistry;

    /**
     * ШАГ 2 для одного стиха. Стих должен быть ANALYZED и иметь сохранённые слова.
     */
    public void analyze(UUID verseId) {
        Verse verse = verseRepository.findByIdAndDeletedAtIsNull(verseId)
                .orElseThrow(() -> new IllegalArgumentException("Verse not found: " + verseId));
        requireAnalyzed(verse);

        runInternalSandhi(List.of(verse), false);
    }

    /**
     * ШАГ 2 для всех ANALYZED-стихов главы (SAME_WORK). Стихи без анализа/слов пропускаются.
     *
     * @return список verseId, отправленных на ШАГ 2
     */
    public List<UUID> analyzeChapter(UUID chapterId) {
        chapterRepository.findByIdAndDeletedAtIsNull(chapterId)
                .orElseThrow(() -> new IllegalArgumentException("Chapter not found: " + chapterId));

        List<Verse> allVerses = verseRepository
                .findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(chapterId);
        List<Verse> versesToAnalyze = allVerses.stream()
                .filter(v -> v.getStatus() == VerseStatus.ANALYZED && hasWords(v))
                .collect(Collectors.toList());

        if (versesToAnalyze.isEmpty()) {
            throw new IllegalStateException("No analyzed verses with words to process in chapter "
                    + chapterId);
        }

        int chunkSize = chunkSize();
        for (int i = 0; i < versesToAnalyze.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, versesToAnalyze.size());
            runInternalSandhi(versesToAnalyze.subList(i, end), true);
        }

        return versesToAnalyze.stream().map(Verse::getId).collect(Collectors.toList());
    }

    /**
     * ШАГ 2 для произвольного списка стихов (MIXED_WORKS). Безусловно обрабатывает
     * только ANALYZED-стихи с сохранёнными словами; прочие пропускаются молча.
     *
     * @return список id реально обработанных стихов (в порядке запроса)
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
            if (verse != null && verse.getStatus() == VerseStatus.ANALYZED && hasWords(verse)) {
                ordered.add(verse);
            }
        }

        int chunkSize = chunkSize();
        for (int i = 0; i < ordered.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, ordered.size());
            runInternalSandhi(ordered.subList(i, end), false);
        }

        return ordered.stream().map(Verse::getId).collect(Collectors.toList());
    }

    private void runInternalSandhi(List<Verse> verses, boolean sameWork) {
        JsonNode llmResponse;
        String rawPrompt;
        try {
            var result = llmClient.callStep2(verses, sameWork);
            llmResponse = result == null ? null : result.response();
            rawPrompt = result == null ? null : result.rawPrompt();
        } catch (Exception e) {
            log.error("LLM STEP 2 failed for {} verses", verses.size(), e);
            return;
        }

        if (llmResponse == null || llmResponse.isNull()) {
            log.error("LLM STEP 2 returned null response for {} verses", verses.size());
            return;
        }

        JsonNode arguments = llmClient.extractStep2Arguments(llmResponse);
        if (arguments == null || arguments.isNull()) {
            log.error("LLM STEP 2 returned no usable arguments for {} verses", verses.size());
            return;
        }

        if (!toolCallValidator.validate(arguments, jsonSchemas.getVerseFormationsStep2Schema())) {
            log.error("LLM STEP 2 arguments failed validation for {} verses", verses.size());
            return;
        }

        JsonNode wordsArray = arguments.get("words");
        if (wordsArray == null || !wordsArray.isArray()) {
            log.error("LLM STEP 2 arguments missing 'words' array");
            return;
        }

        analysisSaver.saveFormations(verses, wordsArray);
        log.info("STEP 2 completed for {} verses", verses.size());
    }

    private void requireAnalyzed(Verse verse) {
        if (verse.getStatus() != VerseStatus.ANALYZED) {
            throw new IllegalStateException("Verse " + verse.getId()
                    + " is not ANALYZED (status=" + verse.getStatus() + ")");
        }
        if (!hasWords(verse)) {
            throw new IllegalStateException("Verse " + verse.getId()
                    + " has no words from STEP 1 to process");
        }
    }

    private boolean hasWords(Verse verse) {
        return !verseWordRepository.findAllByVerse_IdOrderByPositionAsc(verse.getId()).isEmpty();
    }

    /**
     * Размер батча ШАГА 2 (аналогично шагу 1): maxCompletionTokens / tokensPerVerse,
     * но не больше chunkSizeMax; при отсутствии maxCompletionTokens — chunkSizeDefault.
     */
    private int chunkSize() {
        LlmConfigFile.Analysis a = llmConfigRegistry.getAnalysis();
        int chunkSizeMax = a != null && a.chunkSizeMax() != null
                ? a.chunkSizeMax() : CHUNK_SIZE_MAX_DEFAULT;
        int chunkSizeDefault = a != null && a.chunkSizeDefault() != null
                ? a.chunkSizeDefault() : CHUNK_SIZE_DEFAULT_FALLBACK;
        int tokensPerVerse = a != null && a.tokensPerVerse() != null
                ? a.tokensPerVerse() : TOKENS_PER_VERSE_FALLBACK;

        Integer maxTokens = llmProperties.getMaxCompletionTokens();
        if (maxTokens == null || maxTokens <= 0) {
            return chunkSizeDefault;
        }
        int computed = maxTokens / tokensPerVerse;
        if (computed < 1) {
            computed = 1;
        }
        return Math.min(computed, chunkSizeMax);
    }
}
