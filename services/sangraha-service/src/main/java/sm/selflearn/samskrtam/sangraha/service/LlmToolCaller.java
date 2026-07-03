package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sm.selflearn.samskrtam.sangraha.config.LlmConfig;
import sm.selflearn.samskrtam.sangraha.model.Verse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LlmToolCaller {

    private final RestClient llmRestClient;
    private final LlmConfig.LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    private static final String TOOL_NAME = "submit_verse_analysis";
    private static final String SYSTEM_PROMPT = """
        You are a Sanskrit linguistics expert. Your task is to analyze a Sanskrit verse.

        Given the verse text (in devanagari, IAST, or both), you must:
        1. Transcribe between devanagari and IAST if one is missing
        2. Provide Russian and English translations
        3. Analyze sandhi splits (every word boundary, showing original surface form and its components)
        4. For each word, provide full grammatical analysis

        You MUST respond using the 'submit_verse_analysis' tool with ALL fields filled.
        Do NOT respond with free text — only use the tool.
        """;

    /**
     * Вызывает LLM с tool calling.
     * Возвращает полный JSON-ответ от LLM.
     */
    public JsonNode call(Verse verse) {
        String userPrompt = buildUserPrompt(verse);
        var requestBody = buildRequestBody(userPrompt);

        String rawResponse = llmRestClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parseResponse(rawResponse);
    }

    /**
     * Извлекает аргументы вызова submit_verse_analysis из полного ответа LLM.
     */
    public JsonNode extractToolArguments(JsonNode response) {
        try {
            var choices = response.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                var message = choices.get(0).get("message");
                if (message != null) {
                    var toolCalls = message.get("tool_calls");
                    if (toolCalls != null && toolCalls.isArray() && toolCalls.size() > 0) {
                        var function = toolCalls.get(0).get("function");
                        if (function != null && TOOL_NAME.equals(function.get("name").asText())) {
                            var argsStr = function.get("arguments").asText();
                            return objectMapper.readTree(argsStr);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract tool arguments from LLM response", e);
        }
        return null;
    }

    public String extractModelName(JsonNode response) {
        try {
            var model = response.get("model");
            if (model != null) return model.asText();
        } catch (Exception ignored) {}
        return llmProperties.getModel();
    }

    private String buildUserPrompt(Verse verse) {
        var sb = new StringBuilder("Analyze the following Sanskrit verse:\n\n");
        if (verse.getTextDevanagari() != null && !verse.getTextDevanagari().isBlank()) {
            sb.append("Devanagari: ").append(verse.getTextDevanagari()).append("\n");
        }
        if (verse.getTextIast() != null && !verse.getTextIast().isBlank()) {
            sb.append("IAST: ").append(verse.getTextIast()).append("\n");
        }
        sb.append("\nProvide complete analysis using the submit_verse_analysis tool.");
        return sb.toString();
    }

    private Map<String, Object> buildRequestBody(String userPrompt) {
        var toolParams = new HashMap<String, Object>();
        toolParams.put("type", "object");
        toolParams.put("required", List.of(
                "textDevanagari", "textIast", "translationRu", "translationEn", "sandhiSplits", "words"
        ));

        var properties = new HashMap<String, Object>();
        properties.put("textDevanagari", Map.of("type", "string", "description", "Verse text in Devanagari script"));
        properties.put("textIast", Map.of("type", "string", "description", "Verse text in IAST transliteration"));
        properties.put("translationRu", Map.of("type", "string", "description", "Russian translation"));
        properties.put("translationEn", Map.of("type", "string", "description", "English translation"));

        var sandhiItemProps = new HashMap<String, Object>();
        sandhiItemProps.put("surface", Map.of("type", "string"));
        sandhiItemProps.put("components", Map.of(
                "type", "array",
                "items", Map.of("type", "string")
        ));
        properties.put("sandhiSplits", Map.of(
                "type", "array",
                "description", "Analysis of each sandhi split in the verse",
                "items", Map.of(
                        "type", "object",
                        "properties", sandhiItemProps,
                        "required", List.of("surface", "components")
                )
        ));

        var wordItemProps = new HashMap<String, Object>();
        wordItemProps.put("position", Map.of("type", "integer"));
        wordItemProps.put("surfaceIast", Map.of("type", "string"));
        wordItemProps.put("surfaceDevanagari", Map.of("type", "string"));
        wordItemProps.put("lemmaIast", Map.of("type", "string"));
        wordItemProps.put("stem", Map.of("type", "string"));
        wordItemProps.put("root", Map.of("type", "string"));
        wordItemProps.put("pos", Map.of("type", "string", "enum", List.of(
                "NOUN", "VERB", "ADJECTIVE", "ADVERB", "PRONOUN",
                "PARTICLE", "CONJUNCTION", "PREPOSITION", "INTERJECTION", "NUMERAL", "OTHER"
        )));
        wordItemProps.put("gender", Map.of("type", "string", "enum", List.of(
                "MASCULINE", "FEMININE", "NEUTER", "UNSPECIFIED"
        )));
        wordItemProps.put("caseType", Map.of("type", "string", "enum", List.of(
                "NOMINATIVE", "ACCUSATIVE", "INSTRUMENTAL", "DATIVE",
                "ABLATIVE", "GENITIVE", "LOCATIVE", "VOCATIVE", "UNSPECIFIED"
        )));
        wordItemProps.put("numberType", Map.of("type", "string", "enum", List.of(
                "SINGULAR", "DUAL", "PLURAL", "UNSPECIFIED"
        )));
        wordItemProps.put("person", Map.of("type", "string", "enum", List.of(
                "FIRST", "SECOND", "THIRD", "UNSPECIFIED"
        )));
        wordItemProps.put("tense", Map.of("type", "string", "enum", List.of(
                "PRESENT", "IMPERFECT", "AORIST", "PERFECT",
                "PLUPERFECT", "FUTURE", "CONDITIONAL", "BENEDICTIVE", "UNSPECIFIED"
        )));
        wordItemProps.put("mood", Map.of("type", "string", "enum", List.of(
                "INDICATIVE", "IMPERATIVE", "OPTATIVE", "CONDITIONAL", "SUBJUNCTIVE", "UNSPECIFIED"
        )));
        wordItemProps.put("voice", Map.of("type", "string", "enum", List.of(
                "ACTIVE", "MIDDLE", "PASSIVE", "UNSPECIFIED"
        )));
        wordItemProps.put("glossRu", Map.of("type", "string"));
        wordItemProps.put("glossEn", Map.of("type", "string"));

        var wordItem = new HashMap<String, Object>();
        wordItem.put("type", "object");
        wordItem.put("properties", wordItemProps);
        wordItem.put("required", List.of(
                "position", "surfaceIast", "surfaceDevanagari", "lemmaIast",
                "stem", "pos", "glossRu", "glossEn"
        ));

        properties.put("words", Map.of(
                "type", "array",
                "description", "Grammatical analysis of each word",
                "items", wordItem
        ));

        toolParams.put("properties", properties);

        var functionDef = new HashMap<String, Object>();
        functionDef.put("name", TOOL_NAME);
        functionDef.put("description",
                "Submit complete verse analysis: transcription, translation, sandhi splits, and per-word grammar.");
        functionDef.put("parameters", toolParams);

        var tool = Map.of(
                "type", "function",
                "function", functionDef
        );

        var requestBody = new HashMap<String, Object>();
        requestBody.put("model", llmProperties.getModel());
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", SYSTEM_PROMPT),
                Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("tools", List.of(tool));
        requestBody.put("tool_choice", Map.of(
                "type", "function",
                "function", Map.of("name", TOOL_NAME)
        ));

        return requestBody;
    }

    private JsonNode parseResponse(String rawResponse) {
        try {
            return objectMapper.readTree(rawResponse);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse LLM response JSON", e);
        }
    }


}