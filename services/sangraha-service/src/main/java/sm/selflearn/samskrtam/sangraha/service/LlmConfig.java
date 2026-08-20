package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Одна конфигурация подключения к LLM (запись в llm.yaml, ключ — имя модели).
 * Поля со строками могут содержать placeholder'ы вида {@code ${VAR}}, которые
 * резолвятся из Environment (.env) в {@link LlmConfigRegistry} — например
 * {@code api-key: ${SANGRAHA_LLM_API_KEY}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record LlmConfig(
        String baseUrl,
        String apiKey,
        Integer maxCompletionTokens
) {}
