package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Загружает конфигурации подключения к LLM из llm.yaml (корень репозитория).
 * Файл — карта «модель → конфигурация»; активная выбирается по SANGRAHA_LLM_MODEL
 * (переключение модели — правка одной переменной в .env).
 *
 * Строковые поля (base-url, api-key) могут содержать placeholder'ы вида
 * {@code ${VAR}} — они резолвятся из Spring Environment (.env), например
 * {@code api-key: ${SANGRAHA_LLM_API_KEY}}.
 *
 * В {@code @PostConstruct} применяет найденную конфигурацию к {@link LlmProperties}
 * (baseUrl/apiKey/twoPass/maxCompletionTokens). Клиенты LLM (LlmClient,
 * ChapterMetadataClient) аннотированы {@code @DependsOn} этого бина, поэтому
 * порядок гарантирован.
 *
 * Путь к файлу переопределяется через sangraha.llm.config-file / SANGRAHA_LLM_CONFIG_FILE;
 * по умолчанию ищется в рабочей директории, на уровень выше (gradle bootRun) и в /app (Docker).
 */
@Component
@Slf4j
public class LlmConfigRegistry {

    private final LlmProperties llmProperties;
    private final String configFile;
    private final Environment environment;
    private final ObjectMapper yamlMapper = new YAMLMapper();

    private Map<String, LlmConfig> configs = Map.of();

    public LlmConfigRegistry(LlmProperties llmProperties,
                             @Value("${sangraha.llm.config-file:}") String configFile,
                             Environment environment) {
        this.llmProperties = llmProperties;
        this.configFile = configFile;
        this.environment = environment;
    }

    @PostConstruct
    public void applyActiveConfig() {
        LlmConfigFile file = load();
        if (file == null || file.llm() == null || file.llm().configs() == null) {
            log.warn("llm.yaml не найден или не содержит llm.configs — "
                    + "используем baseUrl/twoPass/maxCompletionTokens из env (SANGRAHA_LLM_BASE_URL и т.п.)");
            return;
        }
        configs = file.llm().configs();

        LlmConfig active = configs.get(llmProperties.getModel());
        if (active == null) {
            log.warn("Нет конфигурации в llm.yaml для модели '{}'. Доступны: {}. "
                    + "Используем настройки из env.", llmProperties.getModel(), configs.keySet());
            return;
        }
        apply(active);
        log.info("LLM config applied: model={}, baseUrl={}, twoPass={}, maxCompletionTokens={}",
                llmProperties.getModel(), llmProperties.getBaseUrl(), llmProperties.isTwoPass(),
                llmProperties.getMaxCompletionTokens());
    }

    /** Конфигурация конкретной модели (для тестов и диагностики). */
    public Optional<LlmConfig> findByModel(String model) {
        return Optional.ofNullable(configs.get(model));
    }

    private void apply(LlmConfig config) {
        if (config.baseUrl() != null) llmProperties.setBaseUrl(resolvePlaceholders(config.baseUrl()));
        if (config.apiKey() != null) llmProperties.setApiKey(resolvePlaceholders(config.apiKey()));
        if (config.twoPass() != null) llmProperties.setTwoPass(config.twoPass());
        if (config.maxCompletionTokens() != null) {
            llmProperties.setMaxCompletionTokens(config.maxCompletionTokens());
        }
    }

    /** Резолвит ${VAR} из Environment; значение без placeholder'ов возвращается как есть. */
    private String resolvePlaceholders(String value) {
        return environment.resolvePlaceholders(value);
    }

    private LlmConfigFile load() {
        Path path = resolvePath();
        if (path == null) return null;
        try (InputStream is = Files.newInputStream(path)) {
            return yamlMapper.readValue(is, LlmConfigFile.class);
        } catch (IOException e) {
            log.error("Не удалось прочитать LLM config file: {}", path.toAbsolutePath(), e);
            return null;
        }
    }

    private Path resolvePath() {
        for (String candidate : List.of(configFile, "llm.yaml", "../llm.yaml", "/app/llm.yaml")) {
            if (candidate == null || candidate.isBlank()) continue;
            Path path = Path.of(candidate);
            if (Files.isRegularFile(path)) {
                log.info("Используем LLM config file: {}", path.toAbsolutePath());
                return path;
            }
        }
        return null;
    }
}
