package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Валидатор tool_calls[0].function.arguments по JSON Schema.
 * Не доверяем LLM напрямую — проверяем, что модель вернула корректные поля.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ToolCallValidator {

    private final ObjectMapper objectMapper;

    /**
     * Валидирует аргументы tool_call по JSON Schema.
     *
     * @param arguments  JsonNode с аргументами (извлечён из tool_calls[0].function.arguments)
     * @param jsonSchema JsonNode с JSON Schema для валидации
     * @return true если валидация прошла успешно
     */
    public boolean validate(JsonNode arguments, JsonNode jsonSchema) {
        if (arguments == null) {
            log.warn("Tool call arguments is null");
            return false;
        }

        if (jsonSchema == null) {
            log.warn("JSON Schema is null, skipping validation");
            return false;
        }

        try {
            var factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            var schema = factory.getSchema(jsonSchema);
            Set<ValidationMessage> errors = schema.validate(arguments);

            if (!errors.isEmpty()) {
                log.warn("Tool call arguments validation failed ({} errors):", errors.size());
                for (var error : errors) {
                    log.warn("  - {}", error.getMessage());
                }
                return false;
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to validate tool call arguments", e);
            return false;
        }
    }
}