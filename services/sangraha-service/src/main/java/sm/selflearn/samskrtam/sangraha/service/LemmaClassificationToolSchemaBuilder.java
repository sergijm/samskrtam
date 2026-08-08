package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JSON Schema для tool {@code submit_lemma_classification}
 * (lemma-classification.md §2.3): один параметр {@code classifications} — массив
 * {lemmaId, categoryCode, glossRu, glossEn, confidence?}. {@code confidence} —
 * необязательное поле.
 */
@Component
@RequiredArgsConstructor
public class LemmaClassificationToolSchemaBuilder {

    public static final String TOOL_NAME = "submit_lemma_classification";

    private final ObjectMapper objectMapper;

    public com.fasterxml.jackson.databind.node.ObjectNode buildSchema() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ArrayNode required = params.putArray("required");
        required.add("classifications");

        ObjectNode properties = params.putObject("properties");

        ObjectNode arrayNode = properties.putObject("classifications");
        arrayNode.put("type", "array");
        arrayNode.put("description",
                "One classification per input lemma, matched by lemmaId — categoryCode from the closed "
                        + "category list plus the single most likely gloss");
        ObjectNode item = arrayNode.putObject("items");
        item.put("type", "object");

        ArrayNode itemRequired = item.putArray("required");
        itemRequired.add("lemmaId").add("categoryCode").add("glossRu").add("glossEn");

        ObjectNode itemProps = item.putObject("properties");
        itemProps.putObject("lemmaId").put("type", "string").put("description", "uuid of the lemma");
        itemProps.putObject("categoryCode").put("type", "string")
                .put("description", "one code from the closed 42-category CURRICULUM list");
        itemProps.putObject("glossRu").put("type", "string")
                .put("description", "single most likely Russian gloss (not Devanagari)");
        itemProps.putObject("glossEn").put("type", "string")
                .put("description", "single most likely English gloss (not Devanagari)");
        itemProps.putObject("confidence").put("type", "integer").put("minimum", 0).put("maximum", 100)
                .put("description", "optional 0-100 confidence in the category choice");

        return params;
    }
}