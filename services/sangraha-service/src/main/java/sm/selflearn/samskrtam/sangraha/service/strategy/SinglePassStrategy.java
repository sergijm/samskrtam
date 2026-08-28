package sm.selflearn.samskrtam.sangraha.service.strategy;

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

    private static final String TOOL_NAME_STEP1 = "submit_verse_analyses_step1";
    private static final String TOOL_NAME_STEP2 = "submit_word_formations";

    private final OpenAIClient openAIClient;
    private final LlmPromptBuilder promptBuilder;
    private final LlmToolSchemaBuilder toolSchemaBuilder;
    private final ObjectMapper objectMapper;
    private final String model;
    private final Integer maxCompletionTokens;
    private final LlmStep step;

    @Override
    public String getName() {
        return "single-pass";
    }

    @Override
    public LlmCallResult call(List<Verse> verses, boolean sameWork) throws Exception {
        String toolName;
        String systemPrompt;
        String userPrompt;
        String schemaJson;

        if (step == LlmStep.STEP2) {
            toolName = TOOL_NAME_STEP2;
            systemPrompt = promptBuilder.buildStep2SystemPrompt();
            userPrompt = promptBuilder.buildStep2BatchUserPrompt(verses);
            schemaJson = objectMapper.writeValueAsString(
                    toolSchemaBuilder.buildStep2FunctionDefinitionSchema());
        } else {
            toolName = TOOL_NAME_STEP1;
            systemPrompt = promptBuilder.buildStep1SystemPrompt(verses, sameWork);
            userPrompt = promptBuilder.buildStep1BatchUserPrompt(verses, sameWork);
            schemaJson = objectMapper.writeValueAsString(
                    toolSchemaBuilder.buildStep1FunctionDefinitionSchema());
        }

        FunctionParameters functionParameters = objectMapper.readValue(schemaJson, FunctionParameters.class);

        var functionDefinition = FunctionDefinition.builder()
                .name(toolName)
                .description(step == LlmStep.STEP2
                        ? "Submit STEP 2 internal sandhi (word formation) analyses: for each STEP 1 word, the internal sandhi rule numbers (1–40) and optionally refined derivation fields."
                        : "Submit STEP 1 verse analyses: translation, external sandhi splits, and per-word lexical/morphological analysis (without internal sandhi formationRuleNumbers).")
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
                                        .name(toolName)
                                        .build())
                                .build()
                ));
        if (maxCompletionTokens != null) {
            builder.maxCompletionTokens(maxCompletionTokens);
        }

        ChatCompletionCreateParams params = builder.build();
        String rawPrompt = objectMapper.writeValueAsString(params._body());
        if (log.isDebugEnabled()) {
            log.debug("Single-pass request: {}", rawPrompt);
        }

        ChatCompletion response = openAIClient.chat().completions().create(params);
        log.info("Single-pass LLM call successful, model={}, finishReason={}",
                response.model(),
                response.choices().getFirst().finishReason());

        if (log.isDebugEnabled()) {
            var rawString = objectMapper.writeValueAsString(response);
            log.debug("Single-pass response: {}", rawString);
        }

        return new LlmCallResult(objectMapper.valueToTree(response), rawPrompt);
    }
}