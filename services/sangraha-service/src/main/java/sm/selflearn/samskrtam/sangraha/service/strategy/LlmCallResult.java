package sm.selflearn.samskrtam.sangraha.service.strategy;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Результат вызова LLM-стратегии: сырое дерево ответа + полный промпт (тело запроса),
 * который был отправлен в LLM перед вызовом (для записи в verse_analyses.raw_prompt).
 */
public record LlmCallResult(JsonNode response, String rawPrompt) {
}
