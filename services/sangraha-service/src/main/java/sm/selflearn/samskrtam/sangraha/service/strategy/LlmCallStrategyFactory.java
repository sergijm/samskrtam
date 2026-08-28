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
     * Creates the appropriate strategy. Always single-pass (two-pass removed).
     *
     * @param step этап анализа (STEP1 — translation + external sandhi, STEP2 — internal sandhi)
     */
    public LlmCallStrategy create(OpenAIClient openAIClient, LlmStep step) {
        return new SinglePassStrategy(
                openAIClient,
                promptBuilder,
                toolSchemaBuilder,
                objectMapper,
                llmProperties.getModel(),
                llmProperties.getMaxCompletionTokens(),
                step
        );
    }
}
