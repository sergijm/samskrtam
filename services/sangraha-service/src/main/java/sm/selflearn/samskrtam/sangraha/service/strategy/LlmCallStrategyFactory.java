package sm.selflearn.samskrtam.sangraha.service.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.client.OpenAIClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.service.LlmPromptBuilder;
import sm.selflearn.samskrtam.sangraha.service.LlmProperties;
import sm.selflearn.samskrtam.sangraha.service.LlmToolSchemaBuilder;

@Component
@RequiredArgsConstructor
public class LlmCallStrategyFactory {

    private final LlmProperties llmProperties;
    private final LlmPromptBuilder promptBuilder;
    private final LlmToolSchemaBuilder toolSchemaBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Creates the appropriate strategy based on {@link LlmProperties#isTwoPass()}.
     */
    public LlmCallStrategy create(OpenAIClient openAIClient) {
        if (llmProperties.isTwoPass()) {
            return new TwoPassStrategy(
                    openAIClient,
                    promptBuilder,
                    toolSchemaBuilder,
                    objectMapper,
                    llmProperties.getModel(),
                    llmProperties.getMaxCompletionTokens()
            );
        }
        return new SinglePassStrategy(
                openAIClient,
                promptBuilder,
                toolSchemaBuilder,
                objectMapper,
                llmProperties.getModel(),
                llmProperties.getMaxCompletionTokens()
        );
    }
}
