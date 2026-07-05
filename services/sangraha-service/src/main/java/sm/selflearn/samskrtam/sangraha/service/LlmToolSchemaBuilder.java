package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Строит JSON Schema для tool definition OpenAI SDK (submit_verse_analysis).
 * Содержит enum-ограничения для POS, gender, case, number, person, tense, mood, voice.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmToolSchemaBuilder {

    private final ObjectMapper objectMapper;

    /**
     * Возвращает ObjectNode JSON Schema для FunctionDefinition submit_verse_analysis.
     */
    public ObjectNode buildFunctionDefinitionSchema() {
        ObjectNode params = objectMapper.createObjectNode();
        params.put("type", "object");

        ArrayNode required = params.putArray("required");
        required.add("textDevanagari").add("textIast").add("translationRu")
                .add("translationEn").add("sandhiSplits").add("words");

        ObjectNode properties = params.putObject("properties");
        properties.putObject("textDevanagari").put("type", "string").put("description", "Verse text in Devanagari script");
        properties.putObject("textIast").put("type", "string").put("description", "Verse text in IAST transliteration");
        properties.putObject("translationRu").put("type", "string").put("description", "Russian translation");
        properties.putObject("translationEn").put("type", "string").put("description", "English translation");

        // sandhiSplits
        ObjectNode sandhiItem = properties.putObject("sandhiSplits");
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

        // words
        ObjectNode wordItem = properties.putObject("words");
        wordItem.put("type", "array");
        wordItem.put("description", "Grammatical analysis of each word");
        ObjectNode wordItemObj = wordItem.putObject("items");
        wordItemObj.put("type", "object");

        ArrayNode wordRequired = wordItemObj.putArray("required");
        wordRequired.add("position").add("surfaceIast").add("surfaceDevanagari")
                .add("lemmaIast").add("stem").add("root").add("pos").add("glossRu").add("glossEn")
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
                .add("PARTICLE").add("CONJUNCTION").add("PREPOSITION").add("INTERJECTION").add("NUMERAL").add("OTHER");

        ObjectNode genderField = wordProps.putObject("gender");
        genderField.put("type", "string");
        genderField.putArray("enum")
                .add("MASCULINE").add("FEMININE").add("NEUTER").add("UNSPECIFIED");

        ObjectNode caseField = wordProps.putObject("caseType");
        caseField.put("type", "string");
        caseField.putArray("enum")
                .add("NOMINATIVE").add("ACCUSATIVE").add("INSTRUMENTAL").add("DATIVE")
                .add("ABLATIVE").add("GENITIVE").add("LOCATIVE").add("VOCATIVE").add("UNSPECIFIED");

        ObjectNode numField = wordProps.putObject("numberType");
        numField.put("type", "string");
        numField.putArray("enum")
                .add("SINGULAR").add("DUAL").add("PLURAL").add("UNSPECIFIED");

        ObjectNode personField = wordProps.putObject("person");
        personField.put("type", "string");
        personField.putArray("enum")
                .add("FIRST").add("SECOND").add("THIRD").add("UNSPECIFIED");

        ObjectNode tenseField = wordProps.putObject("tense");
        tenseField.put("type", "string");
        tenseField.putArray("enum")
                .add("PRESENT").add("IMPERFECT").add("AORIST").add("PERFECT")
                .add("PLUPERFECT").add("FUTURE").add("CONDITIONAL").add("BENEDICTIVE").add("UNSPECIFIED");

        ObjectNode moodField = wordProps.putObject("mood");
        moodField.put("type", "string");
        moodField.putArray("enum")
                .add("INDICATIVE").add("IMPERATIVE").add("OPTATIVE").add("CONDITIONAL").add("SUBJUNCTIVE").add("UNSPECIFIED");

        ObjectNode voiceField = wordProps.putObject("voice");
        voiceField.put("type", "string");
        voiceField.putArray("enum")
                .add("ACTIVE").add("MIDDLE").add("PASSIVE").add("UNSPECIFIED");

        wordProps.putObject("glossRu").put("type", "string");
        wordProps.putObject("glossEn").put("type", "string");
        wordProps.putObject("formationRuleNumbers").put("type", "array").putObject("items").put("type", "integer");

        return params;
    }
}