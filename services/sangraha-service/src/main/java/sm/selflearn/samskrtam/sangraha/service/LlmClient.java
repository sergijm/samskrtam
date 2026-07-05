package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.FunctionDefinition;
import com.openai.models.FunctionParameters;
import com.openai.models.chat.completions.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.model.Verse;

import java.util.List;

/**
 * HTTP-клиент к OpenAI API для анализа санскритских стихов через tool calling.
 * Использует официальный OpenAI Java SDK (com.openai:openai-java).
 * Совместим с любым OpenAI-compatible endpoint (ADR-006).
 * Промпт загружается из classpath-ресурса (prompts/verse-analysis.md).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LlmClient {

    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final PromptLoader promptLoader;

    private OpenAIClient openAIClient;

    private static final String TOOL_NAME = "submit_verse_analysis";

    @PostConstruct
    public void init() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .baseUrl(llmProperties.getBaseUrl())
                .apiKey(llmProperties.getApiKey())
                .build();
        log.info("LlmClient initialized with baseUrl={}, model={}", llmProperties.getBaseUrl(), llmProperties.getModel());
    }

    /**
     * Вызывает LLM с tool calling через OpenAI SDK.
     *
     * @return полный ChatCompletion, сериализованный в JsonNode, или null при ошибке
     */
    public JsonNode call(Verse verse) {
        try {
            ChatCompletionCreateParams params = buildParams(verse);
            ChatCompletion response = openAIClient.chat().completions().create(params);
            log.debug("LLM call successful, model={}, finishReason={}",
                    response.model(),
                    response.choices().get(0).finishReason());
            return objectMapper.valueToTree(response);
        } catch (Exception e) {
            log.error("Failed to call LLM API for verse {}", verse.getId(), e);
            return null;
        }
    }

    /**
     * Извлекает аргументы вызова submit_verse_analysis из полного ответа LLM
     * через прямой JSON-парсинг (работает со всеми версиями SDK).
     */
    public JsonNode extractToolArguments(JsonNode response) {
        try {
            var toolCallsNode = response.path("choices")
                    .path(0)
                    .path("message")
                    .path("tool_calls");

            if (toolCallsNode.isMissingNode() || toolCallsNode.isEmpty()) {
                log.warn("No tool_calls in response");
                return null;
            }

            var toolCall = toolCallsNode.get(0);
            var functionNode = toolCall.path("function");
            var name = functionNode.path("name").asText();

            if (!TOOL_NAME.equals(name)) {
                log.warn("Unexpected tool name: {}, expected {}", name, TOOL_NAME);
                return null;
            }

            String argsJson = functionNode.path("arguments").asText();
            if (argsJson == null || argsJson.isEmpty()) {
                log.warn("Empty arguments in tool_call");
                return null;
            }

            return objectMapper.readTree(argsJson);

        } catch (Exception e) {
            log.error("Failed to extract tool arguments from LLM response", e);
            return null;
        }
    }

    /**
     * Извлекает имя модели из ответа LLM.
     */
    public String extractModelName(JsonNode response) {
        try {
            ChatCompletion chatCompletion = objectMapper.treeToValue(response, ChatCompletion.class);
            String model = chatCompletion.model();
            if (!model.isBlank()) {
                return model;
            }
        } catch (Exception e) {
            log.warn("Failed to extract model name from response", e);
        }
        return llmProperties.getModel();
    }

    /**
     * Собирает ChatCompletionCreateParams: system message из ресурса prompts/verse-analysis.md,
     * tool definition (submit_verse_analysis), принудительный tool_choice.
     */
    private ChatCompletionCreateParams buildParams(Verse verse) throws Exception {
        String systemPrompt = extractSystemPrompt(promptLoader.getVerseAnalysisPrompt());
        String userPrompt = buildUserPrompt(verse);

        ObjectNode schemaNode = buildJsonSchema();
        String schemaJson = objectMapper.writeValueAsString(schemaNode);
        FunctionParameters functionParameters = objectMapper.readValue(schemaJson, FunctionParameters.class);

        var functionDefinition = FunctionDefinition.builder()
                .name(TOOL_NAME)
                .description("Submit complete verse analysis: transcription, translation, sandhi splits, and per-word grammar.")
                .parameters(functionParameters)
                .build();

        ChatCompletionTool tool = ChatCompletionTool.ofFunction(
                ChatCompletionFunctionTool.builder()
                        .function(functionDefinition)
                        .build()
        );

        return ChatCompletionCreateParams.builder()
                .model(llmProperties.getModel())
                .addSystemMessage(systemPrompt)
                .addUserMessage(userPrompt)
                .tools(List.of(tool))
                .toolChoice(ChatCompletionToolChoiceOption.ofNamedToolChoice(
                        ChatCompletionNamedToolChoice.builder()
                                .function(ChatCompletionNamedToolChoice.Function.builder()
                                        .name(TOOL_NAME)
                                        .build())
                                .build()
                ))
                .build();
    }

    /**
     * Извлекает содержимое секции ## system из markdown-файла промпта.
     */
    private String extractSystemPrompt(String fullPrompt) {
        int systemStart = fullPrompt.indexOf("## system\n");
        if (systemStart < 0) return fullPrompt;
        int codeStart = fullPrompt.indexOf("```\n", systemStart);
        if (codeStart < 0) return fullPrompt;
        int codeEnd = fullPrompt.indexOf("\n```", codeStart + 5);
        if (codeEnd < 0) return fullPrompt;
        return fullPrompt.substring(codeStart + 5, codeEnd).trim();
    }

    private String buildUserPrompt(Verse verse) {
        var sb = new StringBuilder("Analyze the following Sanskrit verse:\n\n");
        if (verse.getTextDevanagari() != null && !verse.getTextDevanagari().isBlank()) {
            sb.append("Devanagari: ").append(verse.getTextDevanagari()).append("\n");
        }
        if (verse.getTextIast() != null && !verse.getTextIast().isBlank()) {
            sb.append("IAST: ").append(verse.getTextIast()).append("\n");
        }
        sb.append("\nProvide complete analysis using the submit_verse_analysis function.");

        // Добавляем контекст внешних правил сандхи, если они загружены
        JsonNode sandhiRulesNode = promptLoader.getEmenauSandhiRules();
        if (sandhiRulesNode != null) {
            try {
                String sandhiRules = objectMapper.writeValueAsString(sandhiRulesNode);
                if (!sandhiRules.isEmpty()) {
                    sb.append("\n\n---\n");
                    sb.append("External sandhi rules (rules 41–71) for sandhi split reference:\n");
                    sb.append(sandhiRules);
                    sb.append("\n\nIMPORTANT: Only use these external rules (41–71) for sandhi split analysis. ");
                    sb.append("Internal rules (1–40) explain word-internal form changes and must NOT be cited in sandhi splits.");
                }
            } catch (Exception e) {
                log.warn("Failed to serialize sandhi rules for verse {}", verse.getId(), e);
            }
        }

        return sb.toString();
    }

    private ObjectNode buildJsonSchema() {
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