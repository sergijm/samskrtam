package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import com.openai.models.chat.completions.ChatCompletionFunctionTool;
import com.openai.models.chat.completions.ChatCompletionNamedToolChoice;
import com.openai.models.chat.completions.ChatCompletionTool;
import com.openai.models.chat.completions.ChatCompletionToolChoiceOption;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.service.LemmaClassificationPromptBuilder.LemmaBatchItem;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Вызов LLM для классификации батча лемм (lemma-classification.md §2).
 * Отдельный клиент от {@link LlmClient}: другой tool, но тот же OpenAI-стек
 * и та же конфигурация {@code sangraha.llm.*}.
 */
@Component
@DependsOn("llmConfigRegistry")
@RequiredArgsConstructor
@Slf4j
public class LemmaClassificationLlmClient {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final LemmaClassificationPromptBuilder promptBuilder;
    private final LemmaClassificationToolSchemaBuilder toolSchemaBuilder;

    private OpenAIClient openAIClient;

    @PostConstruct
    public void init() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .baseUrl(llmProperties.getBaseUrl())
                .apiKey(llmProperties.getApiKey())
                .build();
        log.info("LemmaClassificationLlmClient initialized with model={}", llmProperties.getModel());
    }

    /**
     * Классифицирует батч — глобальная модель (из LlmProperties).
     */
    public LemmaClassificationSuggestion.BatchResult classifyBatch(List<LemmaBatchItem> items) {
        return classifyBatch(items, null, null);
    }

    /**
     * Классифицирует батч с указанной конфигурацией модели.
     * Если config == null — глобальный {@link LlmProperties}.
     */
    public LemmaClassificationSuggestion.BatchResult classifyBatch(List<LemmaBatchItem> items, LlmConfig config, String model) {
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(items);

        String actualModel = model != null ? model : llmProperties.getModel();
        String actualBaseUrl = config != null && config.baseUrl() != null ? config.baseUrl() : llmProperties.getBaseUrl();
        String actualApiKey = config != null && config.apiKey() != null ? config.apiKey() : llmProperties.getApiKey();
        Integer maxTokens = config != null && config.maxCompletionTokens() != null
                ? config.maxCompletionTokens() : llmProperties.getMaxCompletionTokens();

        log.info("LLM classify: model={}, baseUrl={}, lemmas={}, systemLen={}, userLen={}",
                actualModel, actualBaseUrl, items.size(), systemPrompt.length(), userPrompt.length());
        log.debug("LLM system prompt:\n{}", systemPrompt);
        log.debug("LLM user prompt:\n{}", userPrompt);

        OpenAIClient client = isSameAsGlobal(actualBaseUrl, actualApiKey)
                ? openAIClient
                : OpenAIOkHttpClient.builder().baseUrl(actualBaseUrl).apiKey(actualApiKey).build();

        ChatCompletion response;
        try {
            response = call(client, actualModel, systemPrompt, userPrompt, maxTokens);
        } catch (Exception e) {
            log.error("LLM call failed for batch of {} lemmas", items.size(), e);
            throw new LemmaClassificationCallException("LLM call failed: " + e.getMessage(), e);
        }

        String llmModel = extractModelName(response);
        JsonNode responseNode = objectMapper.valueToTree(response);
        log.info("LLM response: model={}, usage={}", extractModelName(response),
                responseNode.path("usage"));
        log.debug("LLM response body:\n{}", responseNode.toPrettyString());
        JsonNode classifications = extractClassifications(responseNode);
        if (classifications == null) {
            throw new LemmaClassificationCallException("Model did not call submit_lemma_classification or response was unparseable");
        }

        List<LemmaClassificationSuggestion> parsed = parse(classifications);
        log.info("Classified batch: {} lemmas, {} suggestions", items.size(), parsed.size());
        return new LemmaClassificationSuggestion.BatchResult(parsed, llmModel);
    }

    private ChatCompletion call(OpenAIClient client, String model, String systemPrompt,
                                 String userPrompt, Integer maxCompletionTokens) throws Exception {
        String schemaJson = objectMapper.writeValueAsString(toolSchemaBuilder.buildSchema());
        FunctionParameters functionParameters = objectMapper.readValue(schemaJson, FunctionParameters.class);

        var functionDefinition = FunctionDefinition.builder()
                .name(LemmaClassificationToolSchemaBuilder.TOOL_NAME)
                .description("Submit semantic classifications (category + translation) for a batch of Sanskrit lemmas.")
                .parameters(functionParameters)
                .build();

        ChatCompletionTool tool = ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool.builder()
                        .function(functionDefinition)
                        .build()
        );

        var builder = ChatCompletionCreateParams.builder()
                .model(model)
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .tools(List.of(tool))
                .toolChoice(ChatCompletionToolChoiceOption.ofNamedToolChoice(
                        ChatCompletionNamedToolChoice.builder()
                                .function(ChatCompletionNamedToolChoice.Function.builder()
                                        .name(LemmaClassificationToolSchemaBuilder.TOOL_NAME)
                                        .build())
                                .build()
                ));
        if (maxCompletionTokens != null) {
            builder.maxCompletionTokens(maxCompletionTokens);
        }

        return client.chat().completions().create(builder.build());
    }

    private boolean isSameAsGlobal(String baseUrl, String apiKey) {
        return llmProperties.getBaseUrl() != null && llmProperties.getBaseUrl().equals(baseUrl)
                && llmProperties.getApiKey() != null && llmProperties.getApiKey().equals(apiKey);
    }

    private JsonNode extractClassifications(JsonNode response) {
        try {
            var toolCallsNode = response.path("choices")
                    .path(0)
                    .path("message")
                    .path("tool_calls");
            if (toolCallsNode.isMissingNode() || toolCallsNode.isEmpty()) {
                log.warn("No tool_calls in classification response");
                return null;
            }
            var toolCall = toolCallsNode.get(0);
            var functionNode = toolCall.path("function");
            if (!LemmaClassificationToolSchemaBuilder.TOOL_NAME.equals(functionNode.path("name").asText())) {
                log.warn("Unexpected tool name: {}", functionNode.path("name").asText());
                return null;
            }
            String argsJson = functionNode.path("arguments").asText();
            if (argsJson == null || argsJson.isBlank()) {
                log.warn("Empty arguments in classification tool_call");
                return null;
            }
            JsonNode arguments = objectMapper.readTree(argsJson);
            return arguments.get("classifications");
        } catch (Exception e) {
            log.error("Failed to extract classifications from LLM response", e);
            return null;
        }
    }

    private List<LemmaClassificationSuggestion> parse(JsonNode classifications) {
        List<LemmaClassificationSuggestion> result = new ArrayList<>();
        if (!classifications.isArray()) {
            return result;
        }
        for (JsonNode node : classifications) {
            try {
                UUID lemmaId = UUID.fromString(node.path("lemmaId").asText());
                String categoryCode = node.hasNonNull("categoryCode") ? node.path("categoryCode").asText().trim() : null;
                String glossRu = node.hasNonNull("glossRu") ? node.path("glossRu").asText().trim() : null;
                String glossEn = node.hasNonNull("glossEn") ? node.path("glossEn").asText().trim() : null;
                Short confidence = null;
                if (node.hasNonNull("confidence") && !node.path("confidence").asText().isBlank()) {
                    confidence = (short) node.path("confidence").asInt();
                }
                result.add(new LemmaClassificationSuggestion(lemmaId, categoryCode, glossRu, glossEn, confidence));
            } catch (Exception e) {
                log.warn("Skipping malformed classification row: {}", node, e);
            }
        }
        return result;
    }

    private String extractModelName(ChatCompletion response) {
        try {
            String model = response.model();
            if (model != null && !model.isBlank()) {
                return model;
            }
        } catch (Exception e) {
            log.warn("Failed to extract model name from response", e);
        }
        return llmProperties.getModel();
    }

    /** Сигнализирует о неудаче всего батча (§3 шаг 5). */
    public static class LemmaClassificationCallException extends RuntimeException {
        public LemmaClassificationCallException(String message) {
            super(message);
        }

        public LemmaClassificationCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}