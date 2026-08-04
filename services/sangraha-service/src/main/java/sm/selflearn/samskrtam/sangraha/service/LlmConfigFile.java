package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

/**
 * Корень llm.yaml (см. LlmConfigRegistry). Структура:
 * <pre>
 * llm:
 *   configs:
 *     &lt;model&gt;: { base-url, two-pass, max-completion-tokens }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public record LlmConfigFile(Llm llm) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
    public record Llm(Map<String, LlmConfig> configs) {}
}
