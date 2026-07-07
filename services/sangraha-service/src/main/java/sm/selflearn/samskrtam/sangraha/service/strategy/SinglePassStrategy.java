package sm.selflearn.samskrtam.sangraha.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import sm.selflearn.samskrtam.sangraha.model.Verse;
import sm.selflearn.samskrtam.sangraha.service.LlmPromptBuilder;
import sm.selflearn.samskrtam.sangraha.service.LlmToolSchemaBuilder;

import java.util.List;

/**
 * Single-pass strategy: one call with tool_choice forced.
 */
@RequiredArgsConstructor
@Slf4j
public class SinglePassStrategy implements LlmCallStrategy {

    private static final String TOOL_NAME = "submit_verse_analysis";

    private final OpenAIClient openAIClient;
    private final LlmPromptBuilder promptBuilder;
    private final LlmToolSchemaBuilder toolSchemaBuilder;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Integer maxCompletionTokens;

    @Override
    public String getName() {
        return "single-pass";
    }

    @Override
    public JsonNode call(Verse verse) throws Exception {
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
                .model(model)
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
        if (maxCompletionTokens != null) {
            builder.maxCompletionTokens(maxCompletionTokens);
        }

        ChatCompletionCreateParams params = builder.build();
        if (log.isDebugEnabled()) {
            var rawString = objectMapper.writeValueAsString(params._body());
            log.debug("Single-pass request: {}", rawString);
        }

        ChatCompletion response = openAIClient.chat().completions().create(params);
        log.info("Single-pass LLM call successful, model={}, finishReason={}",
                response.model(),
                response.choices().getFirst().finishReason());

        if (log.isDebugEnabled()) {
            var rawString = objectMapper.writeValueAsString(response);
            log.debug("Single-pass response: {}", rawString);
        }

        return objectMapper.valueToTree(response);
    }
}