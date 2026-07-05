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

    // --- Two-pass промпты ---

    /** Промпт первого прохода: свободное рассуждение по шагам (без tool_choice) */
    @Getter
    private String verseAnalysisPass1Prompt;

    /** Промпт второго прохода: форсированный tool call по результатам рассуждения */
    @Getter
    private String verseAnalysisPass2Prompt;

    @PostConstruct
    public void loadPrompts() {
        this.workAnalysisPrompt = load("prompts/work-analysis.md");
        this.verseAnalysisPrompt = load("prompts/verse-analysis.md");
        this.chapterMetadataPrompt = load("prompts/chapter-metadata.md");
        this.emenauSandhiRules = loadJson("prompts/emenau-sandhi-rules.json");
        this.verseAnalysisPass1Prompt = load("prompts/verse-analysis-pass1-reasoning.md");
        this.verseAnalysisPass2Prompt = load("prompts/verse-analysis-pass2-formalize.md");
        log.info("Loaded workAnalysisPrompt ({} chars), verseAnalysisPrompt ({} chars), chapterMetadataPrompt ({} chars), emenauSandhiRules",
            workAnalysisPrompt.length(), verseAnalysisPrompt.length(), chapterMetadataPrompt.length());
        log.info("Loaded pass1Prompt ({} chars), pass2Prompt ({} chars)",
            verseAnalysisPass1Prompt.length(), verseAnalysisPass2Prompt.length());
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