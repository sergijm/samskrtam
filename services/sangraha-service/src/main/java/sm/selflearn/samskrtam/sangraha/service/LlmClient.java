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
import sm.selflearn.samskrtam.sangraha.service.strategy.LlmStep;

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
    private static final String TOOL_NAME_STEP1 = "submit_verse_analyses_step1";
    private static final String TOOL_NAME_STEP2 = "submit_word_formations";

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
     *
     * @param sameWork true для SAME_WORK-батча (все стихи одного произведения)
     */
    public JsonNode call(List<Verse> verses, boolean sameWork) {
        LlmCallResult result = callWithResult(verses, sameWork);
        return result == null ? null : result.response();
    }

    /**
     * Вызывает LLM и возвращает и ответ, и отправленный промпт (для raw_prompt).
     *
     * @param sameWork true для SAME_WORK-батча (все стихи одного произведения)
     */
    public LlmCallResult callWithResult(List<Verse> verses, boolean sameWork) {
        return callStep(verses, sameWork, LlmStep.STEP1);
    }

    /**
     * Вызывает LLM для ШАГА 2 (внутренние сандхи) и возвращает ответ и отправленный промпт.
     * Шаг 2 не запускается автоматически после шага 1 — только по явному запросу.
     *
     * @param sameWork true для SAME_WORK-батча (все стихи одного произведения)
     */
    public LlmCallResult callStep2(List<Verse> verses, boolean sameWork) {
        return callStep(verses, sameWork, LlmStep.STEP2);
    }

    private LlmCallResult callStep(List<Verse> verses, boolean sameWork, LlmStep step) {
        try {
            LlmCallStrategy strategy = strategyFactory.create(openAIClient, step);
            log.debug("Using strategy: {} (step={})", strategy.getName(), step);
            return strategy.call(verses, sameWork);
        } catch (Exception e) {
            log.error("Failed to call LLM API (step={}) for {} verses", step, verses.size(), e);
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

            if (!TOOL_NAME.equals(name) && !TOOL_NAME_STEP1.equals(name)) {
                log.warn("Unexpected tool name: {}, expected {} or {}", name, TOOL_NAME, TOOL_NAME_STEP1);
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
     * Извлекает аргументы вызова submit_word_formations (ШАГ 2) из полного ответа LLM
     * и возвращает объект arguments целиком (содержит массив words), либо null.
     * Объект arguments используется для валидации по JSON Schema и для извлечения words[].
     */
    public JsonNode extractStep2Arguments(JsonNode response) {
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

            if (!TOOL_NAME_STEP2.equals(name)) {
                log.warn("Unexpected tool name: {}, expected {}", name, TOOL_NAME_STEP2);
                return null;
            }

            String argsJson = functionNode.path("arguments").asText();
            if (argsJson == null || argsJson.isEmpty()) {
                log.warn("Empty arguments in tool_call");
                return null;
            }

            return objectMapper.readTree(argsJson);

        } catch (Exception e) {
            log.error("Failed to extract STEP 2 tool arguments from LLM response", e);
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

