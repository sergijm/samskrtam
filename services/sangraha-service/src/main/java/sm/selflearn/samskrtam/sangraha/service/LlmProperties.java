package sm.selflearn.samskrtam.sangraha.service;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "sangraha.llm")
public class LlmProperties {
    private String baseUrl;
    private String apiKey;
    private String model;
    /** Если true — используется two-pass (reasoning → formalize), иначе single-pass (текущее поведение) */
    private boolean twoPass = false;
    /**
     * Максимальное количество токенов на completion-ответ.
     * Если null — используется дефолт провайдера.
     * Рекомендуется 8192 для длинных стихов с two-pass.
     */
    private Long maxCompletionTokens;
}