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
 * Two-pass strategy: first call — free reasoning (no tools), second — formalization with
 * tool_choice forced. Message chain for pass 2:
 * <ol>
 *   <li>system(pass2-formalize)</li>
 *   <li>user(original verse + rules)</li>
 *   <li>assistant(pass1 reasoning)</li>
 *   <li>user(short instruction "submit now")</li>
 * </ol>
 */
@RequiredArgsConstructor
@Slf4j
public class TwoPassStrategy implements LlmCallStrategy {

    private static final String TOOL_NAME = "submit_verse_analysis";

    private final OpenAIClient openAIClient;
    private final LlmPromptBuilder promptBuilder;
    private final LlmToolSchemaBuilder toolSchemaBuilder;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Integer maxCompletionTokens;

    @Override
    public String getName() {
        return "two-pass";
    }

    @Override
    public JsonNode call(Verse verse) throws Exception {
        // --- Pass 1: свободное рассуждение без tool_choice ---
        String pass1SystemPrompt = promptBuilder.extractPass1SystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(verse);
        var pass1Builder = ChatCompletionCreateParams.builder()
                .model(model)
                .addSystemMessage(pass1SystemPrompt)
                .addUserMessage(userPrompt);
        if (maxCompletionTokens != null) {
            pass1Builder.maxCompletionTokens(maxCompletionTokens);
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
                .model(model)
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
        if (maxCompletionTokens != null) {
            pass2Builder.maxCompletionTokens(maxCompletionTokens);
        }
        ChatCompletionCreateParams pass2Params = pass2Builder.build();

        ChatCompletion pass2Response = openAIClient.chat().completions().create(pass2Params);
        log.info("Pass 2 (formalize) for verse {}: finishReason={}",
                verse.getId(),
                pass2Response.choices().get(0).finishReason());

        return objectMapper.valueToTree(pass2Response);
    }
}