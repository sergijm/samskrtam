package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.service.strategy.LlmCallResult;
import sm.selflearn.samskrtam.sangraha.service.strategy.LlmCallStrategy;
import sm.selflearn.samskrtam.sangraha.service.strategy.LlmCallStrategyFactory;

import java.util.List;

/**
 * HTTP-клиент к OpenAI API для анализа санскритских стихов через tool calling.
 * Использует официальный OpenAI Java SDK (com.openai:openai-java).
 * Совместим с любым OpenAI-compatible endpoint (ADR-006).
 *
 * Делегирует вызов LLM через стратегию (single-pass, см. LlmCallStrategy).
 * {@link LlmCallStrategyFactory}.
 */
@Component
@DependsOn("llmConfigRegistry")
@RequiredArgsConstructor
@Slf4j
public class LlmClient {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final LlmPromptBuilder promptBuilder;
    private final LlmToolSchemaBuilder toolSchemaBuilder;
    private final LlmCallStrategyFactory strategyFactory;

    private OpenAIClient openAIClient;

    private static final String TOOL_NAME = "submit_verse_analyses";

    @PostConstruct
    public void init() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .baseUrl(llmProperties.getBaseUrl())
                .apiKey(llmProperties.getApiKey())
                .build();
        log.info("LlmClient initialized with baseUrl={}, model={}, maxCompletionTokens={}",
                llmProperties.getBaseUrl(), llmProperties.getModel(),
                llmProperties.getMaxCompletionTokens());
    }

        /**
     * Вызывает LLM для анализа списка стихов через выбранную стратегию.
     */
    public JsonNode call(List<Verse> verses) {
        LlmCallResult result = callWithResult(verses);
        return result == null ? null : result.response();
    }

    /**
     * Вызывает LLM и возвращает и ответ, и отправленный промпт (для raw_prompt).
     */
    public LlmCallResult callWithResult(List<Verse> verses) {
        try {
            LlmCallStrategy strategy = strategyFactory.create(openAIClient);
            log.debug("Using strategy: {}", strategy.getName());
            return strategy.call(verses);
        } catch (Exception e) {
            log.error("Failed to call LLM API for {} verses", verses.size(), e);
            return null;
        }
    }

    /**
     * Извлекает аргументы вызова submit_verse_analyses из полного ответа LLM
     * и возвращает массив verses (JsonNode ArrayNode) или null.
     */
    public JsonNode extractVersesArguments(JsonNode response) {
        try {
            var toolCallsNode = response.path("choices")
                    .path(0)
                    .path("message")
                    .path("tool_calls");

            if (toolCallsNode.isMissingNode() || toolCallsNode.isEmpty()) {
                log.warn("No tool_calls in response");
                return null;
            }

            var toolCall = toolCallsNode.get(0);
            var functionNode = toolCall.path("function");
            var name = functionNode.path("name").asText();

            if (!TOOL_NAME.equals(name)) {
                log.warn("Unexpected tool name: {}, expected {}", name, TOOL_NAME);
                return null;
            }

            String argsJson = functionNode.path("arguments").asText();
            if (argsJson == null || argsJson.isEmpty()) {
                log.warn("Empty arguments in tool_call");
                return null;
            }

            JsonNode arguments = objectMapper.readTree(argsJson);
            return arguments.get("verses");

        } catch (Exception e) {
            log.error("Failed to extract tool arguments from LLM response", e);
            return null;
        }
    }

    /**
     * Извлекает имя модели из ответа LLM.
     */
    public String extractModelName(JsonNode response) {
        try {
            ChatCompletion chatCompletion = objectMapper.treeToValue(response, ChatCompletion.class);
            String model = chatCompletion.model();
            if (!model.isBlank()) {
                return model;
            }
        } catch (Exception e) {
            log.warn("Failed to extract model name from response", e);
        }
        return llmProperties.getModel();
    }
}

