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

    /** External sandhi rules (правила 41–71) для передачи в контекст LLM */
    @Getter
    private JsonNode emenauSandhiRules;

    @PostConstruct
    public void loadPrompts() {
        this.workAnalysisPrompt = load("prompts/work-analysis.md");
        this.verseAnalysisPrompt = load("prompts/verse-analysis.md");
        this.emenauSandhiRules = loadJson("prompts/emenau-sandhi-rules.json");
        log.info("Loaded workAnalysisPrompt ({} chars), verseAnalysisPrompt ({} chars), emenauSandhiRules",
            workAnalysisPrompt.length(), verseAnalysisPrompt.length());
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