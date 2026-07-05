package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * HTTP-клиент для генерации метаданных произведения (POST /works, §5.2).
 * Один вызов /chat/completions с tool submit_work_metadata.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkMetadataClient {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    private OpenAIClient openAIClient;

    private static final String TOOL_NAME = "submit_work_metadata";

    @PostConstruct
    public void init() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .baseUrl(llmProperties.getBaseUrl())
                .apiKey(llmProperties.getApiKey())
                .build();
        log.info("WorkMetadataClient initialized with baseUrl={}, model={}",
            llmProperties.getBaseUrl(), llmProperties.getModel());
    }

    public JsonNode call(String detectedLanguage, String title, String description) {
        try {
            ChatCompletionCreateParams params = buildParams(detectedLanguage, title, description);
            ChatCompletion response = openAIClient.chat().completions().create(params);
            log.debug("Work metadata LLM call successful, model={}, finishReason={}",
                response.model(), response.choices().get(0).finishReason());
            return objectMapper.valueToTree(response);
        } catch (Exception e) {
            log.error("Failed to call LLM for work metadata (title={})", title, e);
            return null;
        }
    }

    public JsonNode extractToolArguments(JsonNode response) {
        try {
            var toolCallsNode = response.path("choices")
                    .path(0)
                    .path("message")
                    .path("tool_calls");

            if (toolCallsNode.isMissingNode() || toolCallsNode.isEmpty()) {
                log.warn("No tool_calls in work metadata response");
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
                log.warn("Empty arguments in work metadata tool_call");
                return null;
            }

            return objectMapper.readTree(argsJson);
        } catch (Exception e) {
            log.error("Failed to extract work metadata tool arguments", e);
            return null;
        }
    }

    public String extractModelName(JsonNode response) {
        try {
            ChatCompletion chatCompletion = objectMapper.treeToValue(response, ChatCompletion.class);
            String model = chatCompletion.model();
            if (!model.isBlank()) return model;
        } catch (Exception e) {
            log.warn("Failed to extract model name from response", e);
        }
        return llmProperties.getModel();
    }

    private ChatCompletionCreateParams buildParams(String detectedLanguage, String title, String description) throws Exception {
        String systemPrompt = extractSystemPrompt(promptLoader.getWorkAnalysisPrompt());
        String userPrompt = buildUserPrompt(detectedLanguage, title, description);

        ObjectNode schemaNode = buildJsonSchema();
        String schemaJson = objectMapper.writeValueAsString(schemaNode);
        FunctionParameters functionParameters = objectMapper.readValue(schemaJson, FunctionParameters.class);

        var functionDefinition = FunctionDefinition.builder()
                .name(TOOL_NAME)
                .description("Submit complete work metadata: title, description, author in all available languages.")
                .parameters(functionParameters)
                .build();

        ChatCompletionTool tool = ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool.builder()
                        .function(functionDefinition)
                        .build()
        );

        return ChatCompletionCreateParams.builder()
                .model(llmProperties.getModel())
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .tools(List.of(tool))
                .toolChoice(ChatCompletionToolChoiceOption.ofNamedToolChoice(
                        ChatCompletionNamedToolChoice.builder()
                                .function(ChatCompletionNamedToolChoice.Function.builder()
                                        .name(TOOL_NAME)
                                        .build())
                                .build()
                ))
                .build();
    }

    private String extractSystemPrompt(String fullPrompt) {
        // Извлекаем содержимое между ``` в разделе ## system
        int systemStart = fullPrompt.indexOf("## system\n");
        if (systemStart < 0) return fullPrompt;
        int codeStart = fullPrompt.indexOf("```\n", systemStart);
        if (codeStart < 0) return fullPrompt;
        int codeEnd = fullPrompt.indexOf("\n```", codeStart + 5);
        if (codeEnd < 0) return fullPrompt;
        return fullPrompt.substring(codeStart + 5, codeEnd).trim();
    }

    private String buildUserPrompt(String detectedLanguage, String title, String description) {
        var sb = new StringBuilder();
        sb.append("detectedLanguage: ").append(detectedLanguage).append("\n");
        sb.append("title: \"").append(title).append("\"\n");
        sb.append("description: ").append(description != null ? "\"" + description + "\"" : "null").append("\n");
        return sb.toString();
    }

    private ObjectNode buildJsonSchema() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ArrayNode required = params.putArray("required");
        required.add("titleRu").add("titleEn").add("titleSaIast").add("titleSaDevanagari");

        ObjectNode properties = params.putObject("properties");
        properties.putObject("titleRu").put("type", "string").put("description", "Title in Russian");
        properties.putObject("titleEn").put("type", "string").put("description", "Title in English");
        properties.putObject("titleSaIast").put("type", "string").put("description", "Title in Sanskrit (IAST transliteration with diacritics)");
        properties.putObject("titleSaDevanagari").put("type", "string").put("description", "Title in Sanskrit (Devanagari script)");
        properties.putObject("descriptionRu").put("type", "string").put("description", "Description in Russian");
        properties.putObject("descriptionEn").put("type", "string").put("description", "Description in English");
        properties.putObject("author").put("type", "string").put("description", "Author (null if unknown/disputed)");

        return params;
    }
}