package sm.selflearn.samskrtam.sangraha.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты LlmConfigRegistry: загрузка llm.yaml, резолв ${VAR} из .env и применение
 * активной конфигурации к LlmProperties по выбранной модели (SANGRAHA_LLM_MODEL).
 */
class LlmConfigRegistryTest {

    private Path tempConfig;
    private LlmProperties llmProperties;
    private MockEnvironment environment;

    @BeforeEach
    void setUp() throws IOException {
        tempConfig = Files.createTempFile("llm-config-test", ".yaml");
        Files.writeString(tempConfig, """
                llm:
                  configs:
                    claude-sonnet-5:
                      base-url: https://api.aitunnel.ru/v1
                      api-key: ${SANGRAHA_LLM_API_KEY}
                      max-completion-tokens: 128000
                    deepseek-v4-pro:
                      base-url: https://api.aitunnel.ru/v1
                      api-key: ${DEEPSEEK_API_KEY}
                      max-completion-tokens: 8192
                """);
        environment = new MockEnvironment();
        environment.setProperty("SANGRAHA_LLM_API_KEY", "sk-aitunnel-test");
        environment.setProperty("DEEPSEEK_API_KEY", "sk-deepseek-test");
        llmProperties = new LlmProperties();
        llmProperties.setApiKey("fallback-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempConfig);
    }

    @Test
    void applyActiveConfig_matchingModel_appliesSettingsAndResolvesApiKeyFromEnv() {
        llmProperties.setModel("claude-sonnet-5");
        LlmConfigRegistry registry = new LlmConfigRegistry(llmProperties, tempConfig.toString(), environment);

        registry.applyActiveConfig();

        assertThat(llmProperties.getBaseUrl()).isEqualTo("https://api.aitunnel.ru/v1");
        assertThat(llmProperties.getApiKey()).isEqualTo("sk-aitunnel-test");
        assertThat(llmProperties.getMaxCompletionTokens()).isEqualTo(128000);
    }

    @Test
    void applyActiveConfig_anotherModel_usesItsOwnApiKey() {
        llmProperties.setModel("deepseek-v4-pro");
        LlmConfigRegistry registry = new LlmConfigRegistry(llmProperties, tempConfig.toString(), environment);

        registry.applyActiveConfig();

        assertThat(llmProperties.getApiKey()).isEqualTo("sk-deepseek-test");
        assertThat(llmProperties.getMaxCompletionTokens()).isEqualTo(8192);
    }

    @Test
    void applyActiveConfig_unknownModel_leavesEnvSettingsUntouched() {
        llmProperties.setModel("unknown-model");
        llmProperties.setBaseUrl("https://env.example.com/v1");
        LlmConfigRegistry registry = new LlmConfigRegistry(llmProperties, tempConfig.toString(), environment);

        registry.applyActiveConfig();

        assertThat(llmProperties.getBaseUrl()).isEqualTo("https://env.example.com/v1");
        assertThat(llmProperties.getApiKey()).isEqualTo("fallback-key");
    }

    @Test
    void findByModel_returnsConfigOrEmpty() {
        LlmConfigRegistry registry = new LlmConfigRegistry(llmProperties, tempConfig.toString(), environment);
        registry.applyActiveConfig();

        Optional<LlmConfig> claude = registry.findByModel("claude-sonnet-5");
        assertThat(claude).isPresent();
        assertThat(registry.findByModel("nope")).isEmpty();
    }
}
