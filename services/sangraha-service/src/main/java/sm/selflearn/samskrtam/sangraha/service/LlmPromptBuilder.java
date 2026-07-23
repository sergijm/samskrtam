package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.model.Verse;

/**
 * Строит system и user промпты для LLM.
 * Извлекает секцию ## system из markdown-файла промпта.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmPromptBuilder {

    private final ObjectMapper objectMapper;
    private final LlmProperties llmProperties;
    private final PromptLoader promptLoader;

    /**
     * Извлекает содержимое секции ## system из промпта verse-analysis-pass1-reasoning.
     */
    public String extractPass1SystemPrompt() {
        String fullPrompt = promptLoader.getVerseAnalysisPass1Prompt();
        int systemStart = fullPrompt.indexOf("## system\n");
        if (systemStart < 0) return fullPrompt;
        int codeStart = fullPrompt.indexOf("```\n", systemStart);
        if (codeStart < 0) return fullPrompt;
        int codeEnd = fullPrompt.indexOf("\n```", codeStart + 5);
        if (codeEnd < 0) return fullPrompt;
        return fullPrompt.substring(codeStart + 5, codeEnd).trim();
    }

    /**
     * Извлекает содержимое секции ## system из промпта verse-analysis-pass2-formalize.
     */
    public String extractPass2SystemPrompt() {
        String fullPrompt = promptLoader.getVerseAnalysisPass2Prompt();
        int systemStart = fullPrompt.indexOf("## system\n");
        if (systemStart < 0) return fullPrompt;
        int codeStart = fullPrompt.indexOf("```\n", systemStart);
        if (codeStart < 0) return fullPrompt;
        int codeEnd = fullPrompt.indexOf("\n```", codeStart + 5);
        if (codeEnd < 0) return fullPrompt;
        return fullPrompt.substring(codeStart + 5, codeEnd).trim();
    }

    /**
     * Извлекает содержимое секции ## system из markdown-файла промпта verse-analysis.
     */
    public String extractSystemPrompt() {
        String fullPrompt = promptLoader.getVerseAnalysisPrompt();
        int systemStart = fullPrompt.indexOf("## system\n");
        if (systemStart < 0) return fullPrompt;
        int codeStart = fullPrompt.indexOf("```\n", systemStart);
        if (codeStart < 0) return fullPrompt;
        int codeEnd = fullPrompt.indexOf("\n```", codeStart + 5);
        if (codeEnd < 0) return fullPrompt;
        return fullPrompt.substring(codeStart + 5, codeEnd).trim();
    }

    /**
     * Строит user-промпт с текстом стиха и контекстом правил сандхи.
     */
    public String buildUserPrompt(Verse verse) {
        var sb = new StringBuilder("Analyze the following Sanskrit verse:\n\n");
        if (verse.getRawText() != null && !verse.getRawText().isBlank()) {
            sb.append("Devanagari or IAST: ").append(verse.getRawText()).append("\n");
        }

        sb.append("\nProvide complete analysis using the submit_verse_analysis function.");

        JsonNode sandhiRulesNode = promptLoader.getEmenauSandhiRules();
        if (sandhiRulesNode != null) {
            try {
                String sandhiRules = objectMapper.writeValueAsString(sandhiRulesNode);
                if (!sandhiRules.isEmpty()) {
                    sb.append("\n\n---\n");
                    sb.append("External sandhi rules (rules 41–71) for sandhi split reference:\n");
                    sb.append(sandhiRules);
                    sb.append("\n\nIMPORTANT: Only use these external rules (41–71) for sandhi split analysis. ");
                    sb.append("Internal rules (1–40) explain word-internal form changes and must NOT be cited in sandhi splits.");
                }
            } catch (Exception e) {
                log.warn("Failed to serialize sandhi rules for verse {}", verse.getId(), e);
            }
        }

        return sb.toString();
    }
}