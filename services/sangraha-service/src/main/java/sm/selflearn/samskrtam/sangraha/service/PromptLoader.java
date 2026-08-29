package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Загрузчик промптов и контекстных данных из classpath-ресурсов (папка prompts/).
 * Загружается один раз при старте приложения.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromptLoader {

    private final ObjectMapper objectMapper;

    @Getter
    private String workAnalysisPrompt;

    @Getter
    private String verseAnalysisPrompt;

    @Getter
    private String chapterMetadataPrompt;

    /** External sandhi rules (правила 1–71) для передачи в контекст LLM */
    @Getter
    private JsonNode emenauSandhiRules;

    /** Промпт шага 1 (translation + external sandhi + lexical/morphological), prompts/2/step1-translation-external-sandhi.md */
    @Getter
    private String verseAnalysisStep1Prompt;

    /** Промпт шага 2 (внутренние сандхи / словообразование), prompts/2/step2-internal-sandhi.md */
    @Getter
    private String verseAnalysisStep2Prompt;

    /** Внешние правила сандхи (41–71) для шага 1, prompts/2/emenau-sandhi-rules-external.json */
    @Getter
    private JsonNode emenauSandhiRulesExternal;

    /** Внутренние правила сандхи (1–40) для шага 2 (пока не используются), prompts/2/emenau-sandhi-rules-internal.json */
    @Getter
    private JsonNode emenauSandhiRulesInternal;

    /** Промпт классификации лемм (lemma-classification.md §2.1) */
    @Getter
    private String lemmaClassificationPrompt;

    @PostConstruct
    public void loadPrompts() {
        this.workAnalysisPrompt = load("prompts/work-analysis.md");
        this.verseAnalysisPrompt = load("prompts/verse-analysis.md");
        this.chapterMetadataPrompt = load("prompts/chapter-metadata.md");
        this.emenauSandhiRules = loadJson("prompts/emenau-sandhi-rules.json");
        this.verseAnalysisStep1Prompt = load("prompts/2/step1-translation-external-sandhi.md");
        this.verseAnalysisStep2Prompt = load("prompts/2/step2-internal-sandhi.md");
        this.emenauSandhiRulesExternal = loadJson("prompts/2/emenau-sandhi-rules-external.json");
        this.emenauSandhiRulesInternal = loadJson("prompts/2/emenau-sandhi-rules-internal.json");
        this.lemmaClassificationPrompt = load("prompts/lemma-classification.md");
        log.info("Loaded workAnalysisPrompt ({} chars), verseAnalysisPrompt ({} chars), chapterMetadataPrompt ({} chars), emenauSandhiRules",
            workAnalysisPrompt.length(), verseAnalysisPrompt.length(), chapterMetadataPrompt.length());
        log.info("Loaded verseAnalysisStep1Prompt ({} chars), verseAnalysisStep2Prompt ({} chars), emenauSandhiRulesExternal, emenauSandhiRulesInternal",
            verseAnalysisStep1Prompt.length(), verseAnalysisStep2Prompt.length());
        log.info("Loaded lemmaClassificationPrompt ({} chars)", lemmaClassificationPrompt.length());
    }

    private String load(String path) {
        try {
            return new String(new ClassPathResource(path).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load prompt resource: " + path, e);
        }
    }

    private JsonNode loadJson(String path) {
        try {
            return objectMapper.readTree(new ClassPathResource(path).getInputStream());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load JSON resource: " + path, e);
        }
    }
}