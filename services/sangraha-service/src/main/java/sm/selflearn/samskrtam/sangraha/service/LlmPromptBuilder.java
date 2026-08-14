package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.model.Verse;

import java.util.List;

/**
 * Строит system и user промпты для LLM.
 * Извлекает секцию ## system из markdown-файла промпта.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmPromptBuilder {

    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    /**
     * Извлекает содержимое секции ## system из markdown-файла промпта verse-analysis.
     */
    public String extractSystemPrompt() {
        return extractFencedSection(promptLoader.getVerseAnalysisPrompt());
    }

    /**
     * Извлекает текст первого fenced-блока (``` ... ```) внутри секции «## system».
     * Учитывает вложенные code-fence (примеры внутри инструкций) — берётся
     * ПОСЛЕДНЯЯ закрывающая ограда до следующего «## »-заголовка, а не первая.
     */
    private String extractFencedSection(String fullPrompt) {
        if (fullPrompt == null) {
            return "";
        }
        int systemStart = findHeading(fullPrompt, "## system");
        if (systemStart < 0) {
            return fullPrompt.trim();
        }
        int nextSection = fullPrompt.indexOf("\n## ", systemStart + 1);
        String section = nextSection >= 0
                ? fullPrompt.substring(systemStart, nextSection)
                : fullPrompt.substring(systemStart);

        int codeStart = section.indexOf("```\n");
        if (codeStart < 0) {
            return section.trim();
        }
        int contentStart = codeStart + 4; // после "```\n"
        int codeEnd = section.lastIndexOf("\n```");
        if (codeEnd < contentStart) {
            return section.substring(contentStart).trim();
        }
        return section.substring(contentStart, codeEnd).trim();
    }

    /** Ищет заголовок «## system» именно в начале строки (не в упоминаниях внутри текста). */
    private int findHeading(String fullPrompt, String heading) {
        int from = 0;
        while (true) {
            int idx = fullPrompt.indexOf(heading, from);
            if (idx < 0) {
                return -1;
            }
            boolean lineStart = idx == 0 || fullPrompt.charAt(idx - 1) == '\n';
            boolean lineEnd = idx + heading.length() >= fullPrompt.length()
                    || fullPrompt.charAt(idx + heading.length()) == '\n'
                    || fullPrompt.charAt(idx + heading.length()) == '\r';
            if (lineStart && lineEnd) {
                return idx;
            }
            from = idx + 1;
        }
    }

        /**
     * Строит user-промпт с текстом стиха и контекстом правил сандхи (batch-формат).
     */
    public String buildUserPrompt(Verse verse) {
        var sb = new StringBuilder();
        if (verse.getRawText() != null && !verse.getRawText().isBlank()) {
            sb.append("verseIndex: 0\n");
            sb.append("textDevanagari: ").append(verse.getRawText()).append("\n");
            sb.append("textIast: null\n");
        } else {
            sb.append("verseIndex: 0\n");
            sb.append("textDevanagari: null\n");
            if (verse.getTextIast() != null && !verse.getTextIast().isBlank()) {
                sb.append("textIast: ").append(verse.getTextIast()).append("\n");
            } else {
                sb.append("textIast: null\n");
            }
        }

        appendSandhiRules(sb);

        return sb.toString();
    }

    /**
     * Строит batch user-промпт для нескольких стихов.
     * Для каждого стиха выводит блок verseIndex/textDevanagari/textIast.
     * verseIndex = позиция в списке.
     */
    public String buildBatchUserPrompt(List<Verse> verses) {
        var sb = new StringBuilder("Analyze the following Sanskrit verses:\n\n");
        for (int i = 0; i < verses.size(); i++) {
            Verse verse = verses.get(i);
            sb.append("verseIndex: ").append(i).append("\n");
            if (verse.getRawText() != null && !verse.getRawText().isBlank()) {
                sb.append("textDevanagari: ").append(verse.getRawText()).append("\n");
                sb.append("textIast: null\n");
            } else {
                if (verse.getTextDevanagari() != null && !verse.getTextDevanagari().isBlank()) {
                    sb.append("textDevanagari: ").append(verse.getTextDevanagari()).append("\n");
                } else {
                    sb.append("textDevanagari: null\n");
                }
                if (verse.getTextIast() != null && !verse.getTextIast().isBlank()) {
                    sb.append("textIast: ").append(verse.getTextIast()).append("\n");
                } else {
                    sb.append("textIast: null\n");
                }
            }
            sb.append("\n");
        }

        appendSandhiRules(sb);

        return sb.toString();
    }

    private void appendSandhiRules(StringBuilder sb) {
        JsonNode sandhiRulesNode = promptLoader.getEmenauSandhiRules();
        if (sandhiRulesNode != null) {
            try {
                String sandhiRules = objectMapper.writeValueAsString(sandhiRulesNode);
                if (!sandhiRules.isEmpty()) {
                    sb.append("\n---\n");
                    sb.append("External sandhi rules (rules 41–71) for sandhi split reference:\n");
                    sb.append(sandhiRules);
                    sb.append("\n\nIMPORTANT: Only use these external rules (41–71) for sandhi split analysis. ");
                    sb.append("Internal rules (1–40) explain word-internal form changes and must NOT be cited in sandhi splits.");
                }
            } catch (Exception e) {
                log.warn("Failed to serialize sandhi rules", e);
            }
        }
    }
}