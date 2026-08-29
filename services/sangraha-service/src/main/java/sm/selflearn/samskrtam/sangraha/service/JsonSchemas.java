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
    private JsonNode verseAnalysesSchema;

    @Getter
    private JsonNode chapterMetadataSchema;

    /** JSON Schema для submit_verse_analyses_step1 (ШАГ 1, без formationRuleNumbers). */
    @Getter
    private JsonNode verseAnalysesStep1Schema;

    /** JSON Schema для submit_word_formations (ШАГ 2, внутренние сандхи). */
    @Getter
    private JsonNode verseFormationsStep2Schema;

    @PostConstruct
    public void init() {
        this.workMetadataSchema = buildWorkMetadataSchema();
        this.verseAnalysesSchema = buildVerseAnalysesSchema();
        this.chapterMetadataSchema = buildChapterMetadataSchema();
        this.verseAnalysesStep1Schema = buildVerseAnalysesStep1Schema();
        this.verseFormationsStep2Schema = buildVerseFormationsStep2Schema();
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
     * JSON Schema для submit_chapter_metadata tool.
     */
    private ObjectNode buildChapterMetadataSchema() {
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

        schema.putObject("additionalProperties").put("type", "string");

        return schema;
    }

        /**
     * JSON Schema для submit_verse_analyses tool (batch).
     * Корневой объект: { type: object, required: [verses], properties: { verses: [...] } }
     */
    private ObjectNode buildVerseAnalysesSchema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");

        ArrayNode required = schema.putArray("required");
        required.add("verses");

        ObjectNode properties = schema.putObject("properties");

        // verses array
        ObjectNode versesNode = properties.putObject("verses");
        versesNode.put("type", "array");
        versesNode.put("minItems", 1);
        ObjectNode verseItem = versesNode.putObject("items");
        verseItem.put("type", "object");

        ArrayNode verseRequired = verseItem.putArray("required");
        verseRequired.add("verseIndex").add("textIast").add("translationRu")
                .add("translationEn").add("sandhiSplits").add("words");

        ObjectNode verseProps = verseItem.putObject("properties");
        verseProps.putObject("verseIndex").put("type", "integer").put("minimum", 0);
        verseProps.putObject("textDevanagari").put("type", "string")
                .put("description", "Verse text in Devanagari (derived server-side, not supplied by the model)");
        verseProps.putObject("textIast").put("type", "string");
        verseProps.putObject("translationRu").put("type", "string").put("minLength", 1);
        verseProps.putObject("translationEn").put("type", "string").put("minLength", 1);

        // sandhiSplits (inside per-verse)
        ObjectNode sandhiSplits = verseProps.putObject("sandhiSplits");
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

        // words (inside per-verse)
        ObjectNode words = verseProps.putObject("words");
        words.put("type", "array");
        words.put("minItems", 1);
        ObjectNode wordItem = words.putObject("items");
        wordItem.put("type", "object");
        ArrayNode wordRequired = wordItem.putArray("required");
        wordRequired.add("position").add("surfaceIast")
                .add("lemmaIast").add("stem").add("pos").add("glossRu").add("glossEn")
                .add("formType").add("analysisConfidence");

        ObjectNode wordProps = wordItem.putObject("properties");
        wordProps.putObject("position").put("type", "integer").put("minimum", 0);
        wordProps.putObject("surfaceIast").put("type", "string").put("minLength", 1);
        wordProps.putObject("surfaceDevanagari").put("type", "string")
                .put("description", "Surface form in Devanagari (derived server-side, not supplied by the model)");
        wordProps.putObject("lemmaIast").put("type", "string").put("minLength", 1);
        wordProps.putObject("stem").put("type", "string");
        wordProps.putObject("root").put("type", "string");
        wordProps.putObject("pos").put("type", "string");
        wordProps.putObject("formType").put("type", "string");
        wordProps.putObject("isFinite").put("type", "boolean");

        // morphology (nested)
        ObjectNode morphSchema = wordProps.putObject("morphology");
        morphSchema.put("type", "object");
        ObjectNode morphProps = morphSchema.putObject("properties");
        morphProps.putObject("person").put("type", "string");
        morphProps.putObject("number").put("type", "string");
        morphProps.putObject("case").put("type", "string");
        morphProps.putObject("gender").put("type", "string");
        morphProps.putObject("tense").put("type", "string");
        morphProps.putObject("mood").put("type", "string");
        morphProps.putObject("voice").put("type", "string");

        wordProps.putObject("derivationType").put("type", "string");
        wordProps.putObject("derivationalSuffix").put("type", "string");
        wordProps.putObject("derivationalBase").put("type", "string");

        // derivation (nested)
        ObjectNode derivSchema = wordProps.putObject("derivation");
        derivSchema.put("type", "object");
        ObjectNode derivProps = derivSchema.putObject("properties");
        derivProps.putObject("type").put("type", "string");
        derivProps.putObject("suffix").put("type", "string");
        derivProps.putObject("base").put("type", "string");
        derivProps.putObject("description").put("type", "string");

        wordProps.putObject("lemmaGlossRu").put("type", "string");
        wordProps.putObject("lemmaGlossEn").put("type", "string");
        wordProps.putObject("glossRu").put("type", "string").put("minLength", 1);
        wordProps.putObject("glossEn").put("type", "string").put("minLength", 1);
        wordProps.putObject("analysisConfidence").put("type", "string");
        wordProps.putObject("ambiguityNotes").put("type", "string");

        schema.putObject("additionalProperties").put("type", "string");

        return schema;
    }

    /**
     * JSON Schema для submit_verse_analyses_step1 (ШАГ 1). Идентична
     * {@link #buildVerseAnalysesSchema()}, но не содержит formationRuleNumbers
     * (внутренние сандхи — это ШАГ 2).
     */
    private ObjectNode buildVerseAnalysesStep1Schema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");

        ArrayNode required = schema.putArray("required");
        required.add("verses");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode versesNode = properties.putObject("verses");
        versesNode.put("type", "array");
        versesNode.put("minItems", 1);
        ObjectNode verseItem = versesNode.putObject("items");
        verseItem.put("type", "object");

        ArrayNode verseRequired = verseItem.putArray("required");
        verseRequired.add("verseIndex").add("textIast").add("translationRu")
                .add("translationEn").add("sandhiSplits").add("words");

        ObjectNode verseProps = verseItem.putObject("properties");
        verseProps.putObject("verseIndex").put("type", "integer").put("minimum", 0);
        verseProps.putObject("textIast").put("type", "string");
        verseProps.putObject("translationRu").put("type", "string").put("minLength", 1);
        verseProps.putObject("translationEn").put("type", "string").put("minLength", 1);

        ObjectNode sandhiSplits = verseProps.putObject("sandhiSplits");
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

        ObjectNode words = verseProps.putObject("words");
        words.put("type", "array");
        words.put("minItems", 1);
        ObjectNode wordItem = words.putObject("items");
        wordItem.put("type", "object");
        ArrayNode wordRequired = wordItem.putArray("required");
        wordRequired.add("position").add("surfaceIast")
                .add("lemmaIast").add("stem").add("pos").add("glossRu").add("glossEn")
                .add("formType").add("analysisConfidence");

        ObjectNode wordProps = wordItem.putObject("properties");
        wordProps.putObject("position").put("type", "integer").put("minimum", 0);
        wordProps.putObject("surfaceIast").put("type", "string").put("minLength", 1);
        wordProps.putObject("lemmaIast").put("type", "string").put("minLength", 1);
        wordProps.putObject("stem").put("type", "string");
        wordProps.putObject("root").put("type", "string");
        wordProps.putObject("pos").put("type", "string");
        wordProps.putObject("formType").put("type", "string");
        wordProps.putObject("isFinite").put("type", "boolean");

        ObjectNode morphSchema = wordProps.putObject("morphology");
        morphSchema.put("type", "object");
        ObjectNode morphProps = morphSchema.putObject("properties");
        morphProps.putObject("person").put("type", "string");
        morphProps.putObject("number").put("type", "string");
        morphProps.putObject("case").put("type", "string");
        morphProps.putObject("gender").put("type", "string");
        morphProps.putObject("tense").put("type", "string");
        morphProps.putObject("mood").put("type", "string");
        morphProps.putObject("voice").put("type", "string");

        wordProps.putObject("derivationType").put("type", "string");
        wordProps.putObject("derivationalSuffix").put("type", "string");
        wordProps.putObject("derivationalBase").put("type", "string");

        ObjectNode derivSchema = wordProps.putObject("derivation");
        derivSchema.put("type", "object");
        ObjectNode derivProps = derivSchema.putObject("properties");
        derivProps.putObject("type").put("type", "string");
        derivProps.putObject("suffix").put("type", "string");
        derivProps.putObject("base").put("type", "string");
        derivProps.putObject("description").put("type", "string");

        wordProps.putObject("lemmaGlossRu").put("type", "string");
        wordProps.putObject("lemmaGlossEn").put("type", "string");
        wordProps.putObject("glossRu").put("type", "string").put("minLength", 1);
        wordProps.putObject("glossEn").put("type", "string").put("minLength", 1);
        wordProps.putObject("analysisConfidence").put("type", "string");
        wordProps.putObject("ambiguityNotes").put("type", "string");

        schema.putObject("additionalProperties").put("type", "string");

        return schema;
    }

    /**
     * JSON Schema для submit_word_formations (ШАГ 2, внутренние сандхи).
     * Корневой объект: { type: object, required: [words], properties: { words: [...] } },
     * words — плоский массив, каждый элемент с verseIndex/position/formationRuleNumbers.
     */
    private ObjectNode buildVerseFormationsStep2Schema() {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");

        ArrayNode required = schema.putArray("required");
        required.add("words");

        ObjectNode properties = schema.putObject("properties");

        ObjectNode wordsNode = properties.putObject("words");
        wordsNode.put("type", "array");
        wordsNode.put("minItems", 1);
        ObjectNode wordItem = wordsNode.putObject("items");
        wordItem.put("type", "object");

        ArrayNode wordRequired = wordItem.putArray("required");
        wordRequired.add("verseIndex").add("position").add("surfaceIast").add("formationRuleNumbers");

        ObjectNode wordProps = wordItem.putObject("properties");
        wordProps.putObject("verseIndex").put("type", "integer").put("minimum", 0);
        wordProps.putObject("position").put("type", "integer").put("minimum", 0);
        wordProps.putObject("surfaceIast").put("type", "string").put("minLength", 1);
        wordProps.putObject("lemmaIast").put("type", "string");
        wordProps.putObject("root").put("type", "string");
        wordProps.putObject("derivationalBase").put("type", "string");
        wordProps.putObject("derivationalSuffix").put("type", "string");

        ObjectNode formationRuleNumbers = wordProps.putObject("formationRuleNumbers");
        formationRuleNumbers.put("type", "array");
        formationRuleNumbers.putObject("items").put("type", "integer");

        ObjectNode formationConfidence = wordProps.putObject("formationConfidence");
        formationConfidence.put("type", "string");
        formationConfidence.putArray("enum").add("HIGH").add("MEDIUM").add("LOW");

        wordProps.putObject("formationNotes").put("type", "string");
        wordProps.putObject("formationExplanation").put("type", "string");

        schema.putObject("additionalProperties").put("type", "string");

        return schema;
    }
}