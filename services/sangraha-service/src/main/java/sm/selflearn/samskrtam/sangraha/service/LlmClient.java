package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.model.Verse;

import java.util.List;

/**
 * HTTP-клиент к OpenAI API для анализа санскритских стихов через tool calling.
 * Использует официальный OpenAI Java SDK (com.openai:openai-java).
 * Совместим с любым OpenAI-compatible endpoint (ADR-006).
 * <p>
 * Поддерживает два режима работы, переключаемых флагом {@code llmProperties.twoPass}:
 * <ul>
 *   <li>{@code twoPass=false} (по умолчанию) — single-pass: один вызов с {@code tool_choice}=forced на
 *       {@code submit_verse_analysis} с промптом {@code verse-analysis.md}.</li>
 *   <li>{@code twoPass=true} — two-pass: первый вызов без tool_choice (свободное рассуждение
 *       по шагам, промпт {@code verse-analysis-pass1-reasoning.md}), затем второй вызов
 *       с {@code tool_choice}=forced (формализация, промпт {@code verse-analysis-pass2-formalize.md}).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmClient {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final LlmPromptBuilder promptBuilder;
    private final LlmToolSchemaBuilder toolSchemaBuilder;

    private OpenAIClient openAIClient;

    private static final String TOOL_NAME = "submit_verse_analysis";

    @PostConstruct
    public void init() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .baseUrl(llmProperties.getBaseUrl())
                .apiKey(llmProperties.getApiKey())
                .build();
        log.info("LlmClient initialized with baseUrl={}, model={}, twoPass={}, maxCompletionTokens={}",
                llmProperties.getBaseUrl(), llmProperties.getModel(), llmProperties.isTwoPass(),
                llmProperties.getMaxCompletionTokens());
    }

    /**
     * Вызывает LLM для анализа стиха — в зависимости от {@code llmProperties.twoPass}.
     * <ul>
     *   <li>{@code twoPass=false}: текущее поведение single-pass.</li>
     *   <li>{@code twoPass=true}: two-pass (reasoning + formalize).</li>
     * </ul>
     */
    public JsonNode call(Verse verse) {
        if (llmProperties.isTwoPass()) {
            return callTwoPass(verse);
        }
        return callSinglePass(verse);
    }

    /**
     * Single-pass: один вызов с tool_choice forced.
     */
    private JsonNode callSinglePass(Verse verse) {
        try {
            ChatCompletionCreateParams params = buildSinglePassParams(verse);
            if (log.isDebugEnabled()) {
                var rawString = objectMapper.writeValueAsString(params._body());
                log.debug(rawString);
            }

            ChatCompletion response = openAIClient.chat().completions().create(params);
            log.info("Single-pass LLM call successful, model={}, finishReason={}",
                    response.model(),
                    response.choices().getFirst().finishReason());

            if (log.isDebugEnabled()) {
                var rawString = objectMapper.writeValueAsString(response);
                log.debug(rawString);
            }

            return objectMapper.valueToTree(response);
        } catch (Exception e) {
            log.error("Failed to call LLM API (single-pass) for verse {}", verse.getId(), e);
            return null;
        }
    }

    /**
     * Two-pass: первый вызов — свободное рассуждение (без tools), второй — формализация с tool_choice forced.
     * Оба прохода логируются отдельно.
     * <p>
     * Цепочка сообщений для второго вызова:
     * <ol>
     *   <li>system(pass2-formalize)</li>
     *   <li>user(оригинальный стих + правила)</li>
     *   <li>assistant(ответ pass1 — свободное рассуждение)</li>
     *   <li>user(краткая инструкция "submit now")</li>
     * </ol>
     */
    private JsonNode callTwoPass(Verse verse) {
        try {
            // --- Pass 1: свободное рассуждение без tool_choice ---
            String pass1SystemPrompt = promptBuilder.extractPass1SystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(verse);
            var pass1Builder = ChatCompletionCreateParams.builder()
                    .model(llmProperties.getModel())
                    .addSystemMessage(pass1SystemPrompt)
                    .addUserMessage(userPrompt);
            if (llmProperties.getMaxCompletionTokens() != null) {
                pass1Builder.maxCompletionTokens(llmProperties.getMaxCompletionTokens());
            }
            ChatCompletionCreateParams pass1Params = pass1Builder.build();
            ChatCompletion pass1Response = openAIClient.chat().completions().create(pass1Params);
            String pass1Content = pass1Response.choices().get(0).message().content().orElse("");
            log.info("Pass 1 (reasoning) for verse {}: {} chars, finishReason={}",
                    verse.getId(), pass1Content.length(),
                    pass1Response.choices().get(0).finishReason());

            // --- Pass 2: формализация с tool_choice forced ---
            String pass2SystemPrompt = promptBuilder.extractPass2SystemPrompt();
            String pass2UserInstruction = "Please submit your analysis above through submit_verse_analysis now, " +
                    "following the field-by-field mapping in the system instructions.";

            String schemaJson = objectMapper.writeValueAsString(toolSchemaBuilder.buildFunctionDefinitionSchema());
            FunctionParameters functionParameters = objectMapper.readValue(schemaJson, FunctionParameters.class);

            var functionDefinition = FunctionDefinition.builder()
                    .name(TOOL_NAME)
                    .description("Submit complete verse analysis: transcription, translation, sandhi splits, and per-word grammar.")
                    .parameters(functionParameters)
                    .build();

            ChatCompletionTool tool = ChatCompletionTool.ofFunction(
                    ChatCompletionFunctionTool.builder()
                            .function(functionDefinition)
                            .build()
            );

            var pass2Builder = ChatCompletionCreateParams.builder()
                    .model(llmProperties.getModel())
                    .addSystemMessage(pass2SystemPrompt)
                    .addUserMessage(promptBuilder.buildUserPrompt(verse))
                    .addAssistantMessage(pass1Content)
                    .addUserMessage(pass2UserInstruction)
                    .tools(List.of(tool))
                    .toolChoice(ChatCompletionToolChoiceOption.ofNamedToolChoice(
                            ChatCompletionNamedToolChoice.builder()
                                    .function(ChatCompletionNamedToolChoice.Function.builder()
                                            .name(TOOL_NAME)
                                            .build())
                                    .build()
                    ));
            if (llmProperties.getMaxCompletionTokens() != null) {
                pass2Builder.maxCompletionTokens(llmProperties.getMaxCompletionTokens());
            }
            ChatCompletionCreateParams pass2Params = pass2Builder.build();

            ChatCompletion pass2Response = openAIClient.chat().completions().create(pass2Params);
            log.info("Pass 2 (formalize) for verse {}: finishReason={}",
                    verse.getId(),
                    pass2Response.choices().get(0).finishReason());

            return objectMapper.valueToTree(pass2Response);

        } catch (Exception e) {
            log.error("Failed to call LLM API (two-pass) for verse {}", verse.getId(), e);
            return null;
        }
    }

    /**
     * Извлекает аргументы вызова submit_verse_analysis из полного ответа LLM
     * через прямой JSON-парсинг (работает со всеми версиями SDK).
     */
    public JsonNode extractToolArguments(JsonNode response) {
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

            return objectMapper.readTree(argsJson);

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

    /**
     * Собирает ChatCompletionCreateParams для single-pass:
     * system message из LlmPromptBuilder,
     * tool definition (submit_verse_analysis) из LlmToolSchemaBuilder,
     * принудительный tool_choice.
     */
private ChatCompletionCreateParams buildSinglePassParams(Verse verse) throws Exception {
        String systemPrompt = promptBuilder.extractSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(verse);

        String schemaJson = objectMapper.writeValueAsString(toolSchemaBuilder.buildFunctionDefinitionSchema());
        FunctionParameters functionParameters = objectMapper.readValue(schemaJson, FunctionParameters.class);

        var functionDefinition = FunctionDefinition.builder()
                .name(TOOL_NAME)
                .description("Submit complete verse analysis: transcription, translation, sandhi splits, and per-word grammar.")
                .parameters(functionParameters)
                .build();

        ChatCompletionTool tool = ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool.builder()
                        .function(functionDefinition)
                        .build()
        );

        var builder = ChatCompletionCreateParams.builder()
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
                ));
        if (llmProperties.getMaxCompletionTokens() != null) {
            builder.maxCompletionTokens(llmProperties.getMaxCompletionTokens());
        }
        return builder.build();
    }


}