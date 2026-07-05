package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * JSON Schemas для валидации tool_calls.
 * Строятся один раз при старте, используются для валидации аргументов LLM.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JsonSchemas {

    private final ObjectMapper objectMapper;

    @Getter
    private JsonNode workMetadataSchema;

    @Getter
    private JsonNode verseAnalysisSchema;

    @PostConstruct
    public void init() {
        this.workMetadataSchema = buildWorkMetadataSchema();
        this.verseAnalysisSchema = buildVerseAnalysisSchema();
        log.info("JSON Schemas initialized");
    }

    /**
     * JSON Schema для submit_work_metadata tool.
     */
    private ObjectNode buildWorkMetadataSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");

        ArrayNode required = schema.putArray("required");
        required.add("titleRu").add("titleEn").add("titleSaIast").add("titleSaDevanagari");

        ObjectNode properties = schema.putObject("properties");
        properties.putObject("titleRu").put("type", "string").put("minLength", 1);
        properties.putObject("titleEn").put("type", "string").put("minLength", 1);
        properties.putObject("titleSaIast").put("type", "string").put("minLength", 1);
        properties.putObject("titleSaDevanagari").put("type", "string").put("minLength", 1);
        properties.putObject("descriptionRu").put("type", "string");
        properties.putObject("descriptionEn").put("type", "string");
        properties.putObject("author").put("type", "string");

        schema.putObject("additionalProperties").put("type", "string");

        return schema;
    }

    /**
     * JSON Schema для submit_verse_analysis tool.
     */
    private ObjectNode buildVerseAnalysisSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");

        ArrayNode required = schema.putArray("required");
        required.add("textDevanagari").add("textIast").add("translationRu")
                .add("translationEn").add("sandhiSplits").add("words");

        ObjectNode properties = schema.putObject("properties");
        properties.putObject("textDevanagari").put("type", "string");
        properties.putObject("textIast").put("type", "string");
        properties.putObject("translationRu").put("type", "string").put("minLength", 1);
        properties.putObject("translationEn").put("type", "string").put("minLength", 1);

        // sandhiSplits
        ObjectNode sandhiSplits = properties.putObject("sandhiSplits");
        sandhiSplits.put("type", "array");
        sandhiSplits.put("minItems", 0);
        ObjectNode sandhiItem = sandhiSplits.putObject("items");
        sandhiItem.put("type", "object");
        ArrayNode sandhiRequired = sandhiItem.putArray("required");
        sandhiRequired.add("surface").add("components");
        ObjectNode sandhiProps = sandhiItem.putObject("properties");
        sandhiProps.putObject("surface").put("type", "string").put("minLength", 1);
        ObjectNode components = sandhiProps.putObject("components");
        components.put("type", "array");
        components.put("minItems", 1);
        components.putObject("items").put("type", "string");

        // words
        ObjectNode words = properties.putObject("words");
        words.put("type", "array");
        words.put("minItems", 1);
        ObjectNode wordItem = words.putObject("items");
        wordItem.put("type", "object");
        ArrayNode wordRequired = wordItem.putArray("required");
        wordRequired.add("position").add("surfaceIast").add("surfaceDevanagari")
                .add("lemmaIast").add("stem").add("pos").add("glossRu").add("glossEn");

        ObjectNode wordProps = wordItem.putObject("properties");
        wordProps.putObject("position").put("type", "integer").put("minimum", 0);
        wordProps.putObject("surfaceIast").put("type", "string").put("minLength", 1);
        wordProps.putObject("surfaceDevanagari").put("type", "string").put("minLength", 1);
        wordProps.putObject("lemmaIast").put("type", "string").put("minLength", 1);
        wordProps.putObject("stem").put("type", "string").put("minLength", 1);
        wordProps.putObject("root").put("type", "string");
        wordProps.putObject("pos").put("type", "string");
        wordProps.putObject("gender").put("type", "string");
        wordProps.putObject("caseType").put("type", "string");
        wordProps.putObject("numberType").put("type", "string");
        wordProps.putObject("person").put("type", "string");
        wordProps.putObject("tense").put("type", "string");
        wordProps.putObject("mood").put("type", "string");
        wordProps.putObject("voice").put("type", "string");
        wordProps.putObject("glossRu").put("type", "string").put("minLength", 1);
        wordProps.putObject("glossEn").put("type", "string").put("minLength", 1);

        schema.putObject("additionalProperties").put("type", "string");

        return schema;
    }
}