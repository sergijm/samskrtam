package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Строит JSON Schema для tool definition OpenAI SDK (submit_verse_analyses).
 * Содержит enum-ограничения для POS, gender, case, number, person, tense, mood, voice.
 * Корневой объект: свойство `verses` — массив per-verse объектов с verseIndex.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmToolSchemaBuilder {

    private final ObjectMapper objectMapper;

    /**
     * Возвращает ObjectNode JSON Schema для FunctionDefinition submit_verse_analyses.
     * Корневой объект: { type: object, required: [verses], properties: { verses: [...] } }
     */
    public ObjectNode buildBatchFunctionDefinitionSchema() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ArrayNode required = params.putArray("required");
        required.add("verses");

        ObjectNode properties = params.putObject("properties");

        // verses array
        ObjectNode versesNode = properties.putObject("verses");
        versesNode.put("type", "array");
        versesNode.put("description",
                "Array of verse analyses — one entry per input verse, matched by verseIndex");
        ObjectNode verseItem = versesNode.putObject("items");
        verseItem.put("type", "object");

        ArrayNode verseRequired = verseItem.putArray("required");
        verseRequired.add("verseIndex").add("textDevanagari").add("textIast").add("translationRu")
                .add("translationEn").add("sandhiSplits").add("words");

        ObjectNode verseProps = verseItem.putObject("properties");
        verseProps.putObject("verseIndex").put("type", "integer")
                .put("description", "0-based index of the verse in the input batch");

        verseProps.putObject("textDevanagari").put("type", "string")
                .put("description", "Verse text in Devanagari script");
        verseProps.putObject("textIast").put("type", "string")
                .put("description", "Verse text in IAST transliteration");
        verseProps.putObject("translationRu").put("type", "string")
                .put("description", "Russian translation");
        verseProps.putObject("translationEn").put("type", "string")
                .put("description", "English translation");

        // sandhiSplits (inside per-verse)
        ObjectNode sandhiItem = verseProps.putObject("sandhiSplits");
        sandhiItem.put("type", "array");
        sandhiItem.put("description", "Analysis of each sandhi split in the verse");
        ObjectNode sandhiItemObj = sandhiItem.putObject("items");
        sandhiItemObj.put("type", "object");
        ArrayNode sandhiRequired = sandhiItemObj.putArray("required");
        sandhiRequired.add("surface").add("components").add("ruleNumbers");
        ObjectNode sandhiProps = sandhiItemObj.putObject("properties");
        sandhiProps.putObject("surface").put("type", "string");
        sandhiProps.putObject("components").put("type", "array").putObject("items").put("type", "string");
        sandhiProps.putObject("ruleNumbers").put("type", "array").putObject("items").put("type", "integer");

                        // words (inside per-verse)
        ObjectNode wordItem = verseProps.putObject("words");
        wordItem.put("type", "array");
        wordItem.put("description", "Grammatical analysis of each word");
        ObjectNode wordItemObj = wordItem.putObject("items");
        wordItemObj.put("type", "object");

        ArrayNode wordRequired = wordItemObj.putArray("required");
        wordRequired.add("position").add("surfaceIast").add("surfaceDevanagari")
                .add("lemmaIast").add("stem").add("pos").add("glossRu").add("glossEn")
                .add("formType").add("analysisConfidence")
                .add("formationRuleNumbers");

        ObjectNode wordProps = wordItemObj.putObject("properties");
        wordProps.putObject("position").put("type", "integer");
        wordProps.putObject("surfaceIast").put("type", "string");
        wordProps.putObject("surfaceDevanagari").put("type", "string");
        wordProps.putObject("lemmaIast").put("type", "string");
        wordProps.putObject("stem").put("type", "string");
        wordProps.putObject("root").put("type", "string");

        ObjectNode posField = wordProps.putObject("pos");
        posField.put("type", "string");
        posField.putArray("enum")
                .add("NOUN").add("VERB").add("ADJECTIVE").add("ADVERB").add("PRONOUN")
                .add("PARTICLE").add("CONJUNCTION").add("INDECLINABLE").add("INTERJECTION").add("NUMERAL").add("OTHER");

        // formType
        ObjectNode formTypeField = wordProps.putObject("formType");
        formTypeField.put("type", "string");
        formTypeField.putArray("enum")
                .add("FINITE").add("INFINITIVE").add("ABSOLUTIVE").add("PARTICIPLE")
                .add("GERUNDIVE").add("OTHER_NONFINITE").add("NOMINAL").add("ADJECTIVAL")
                .add("PRONOMINAL").add("INDECLINABLE");

        wordProps.putObject("isFinite").put("type", "boolean");

        // morphology (nested object)
        ObjectNode morphologyField = wordProps.putObject("morphology");
        morphologyField.put("type", "object");
        ObjectNode morphProps = morphologyField.putObject("properties");
        morphProps.putObject("person").put("type", "string");
        morphProps.putObject("number").put("type", "string");
        morphProps.putObject("case").put("type", "string");
        morphProps.putObject("gender").put("type", "string");
        morphProps.putObject("tense").put("type", "string");
        morphProps.putObject("mood").put("type", "string");
        morphProps.putObject("voice").put("type", "string");

        // derivation flat fields
        ObjectNode derivTypeField = wordProps.putObject("derivationType");
        derivTypeField.put("type", "string");
        derivTypeField.putArray("enum")
                .add("SIMPLE_INFLECTION").add("ABSOLUTIVE").add("PARTICIPLE").add("GERUNDIVE")
                .add("INFINITIVE").add("CAUSATIVE").add("DESIDERATIVE").add("DENOMINATIVE")
                .add("COMPOUND_VERB").add("OTHER");
        wordProps.putObject("derivationalSuffix").put("type", "string");
        wordProps.putObject("derivationalBase").put("type", "string");

        // derivation nested object
        ObjectNode derivationField = wordProps.putObject("derivation");
        derivationField.put("type", "object");
        ObjectNode derivProps = derivationField.putObject("properties");
        derivProps.putObject("type").put("type", "string");
        derivProps.putObject("suffix").put("type", "string");
        derivProps.putObject("base").put("type", "string");
        derivProps.putObject("description").put("type", "string");

        // lemma glosses
        wordProps.putObject("lemmaGlossRu").put("type", "string");
        wordProps.putObject("lemmaGlossEn").put("type", "string");

        wordProps.putObject("glossRu").put("type", "string");
        wordProps.putObject("glossEn").put("type", "string");
        wordProps.putObject("formationRuleNumbers").put("type", "array").putObject("items").put("type", "integer");

        // analysis confidence
        ObjectNode confidenceField = wordProps.putObject("analysisConfidence");
        confidenceField.put("type", "string");
        confidenceField.putArray("enum")
                .add("HIGH").add("MEDIUM").add("LOW");

        wordProps.putObject("ambiguityNotes").put("type", "string");

        return params;
    }
}