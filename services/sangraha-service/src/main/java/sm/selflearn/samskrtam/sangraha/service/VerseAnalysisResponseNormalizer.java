package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Нормализует сырой ответ LLM до массива verses[] (JsonNode ArrayNode) или null.
 *
 * <p>В зависимости от модели / gateway / tool-call parsing реально могут прийти разные формы:
 * <pre>
 *   [...]
 *   {"verses":[...]}
 *   {"verses":"{\"verses\":[...]}"}
 *   "{\"verses\":[...]}"
 *   {"arguments":"{\"verses\":[...]}"}
 *   "```json\n{\"verses\":[...]}\n```"
 * </pre>
 * Типу JsonNode доверять нельзя — всё сводится к массиву verses.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerseAnalysisResponseNormalizer {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    /**
     * Извлекает из LLM-ответа массив verses (возможно, обёрнутый) и нормализует его.
     *
     * @return ArrayNode verses или null, если ответ не содержит пригодного массива
     */
    public JsonNode normalizeToVersesArray(JsonNode llmResponse) {
        JsonNode versesArrayNode;
        try {
            versesArrayNode = llmClient.extractVersesArguments(llmResponse);

            if (log.isDebugEnabled()) {
                log.debug("Extracted LLM arguments: {}",
                        objectMapper.writeValueAsString(versesArrayNode));
            }

            /*
             * Максимальное количество итераций защиты от неожиданной
             * рекурсивной/циклической обёртки.
             */
            for (int depth = 0; depth < 10 && versesArrayNode != null; depth++) {

                if (versesArrayNode.isNull()) {
                    versesArrayNode = null;
                    break;
                }

                // Уже то, что нам нужно.
                if (versesArrayNode.isArray()) {
                    break;
                }

                /*
                 * Строка может содержать JSON:
                 *
                 * "{\"verses\":[...]}"
                 *
                 * или markdown:
                 *
                 * ```json
                 * {"verses":[...]}
                 * ```
                 */
                if (versesArrayNode.isTextual()) {
                    String text = versesArrayNode.asText();

                    if (text == null || text.isBlank()) {
                        versesArrayNode = null;
                        break;
                    }

                    text = text.trim();

                    // Убираем markdown code fence, если модель его добавила.
                    if (text.startsWith("```")) {
                        int firstNewline = text.indexOf('\n');
                        int lastFence = text.lastIndexOf("```");

                        if (firstNewline >= 0 && lastFence > firstNewline) {
                            text = text.substring(firstNewline + 1, lastFence).trim();
                        }
                    }

                    try {
                        versesArrayNode = objectMapper.readTree(text);
                        continue;
                    } catch (Exception e) {
                        log.warn("LLM returned textual value which is not valid JSON: {}",
                                text.length() > 500
                                        ? text.substring(0, 500) + "..."
                                        : text);
                        versesArrayNode = null;
                        break;
                    }
                }

                /*
                 * Объект.
                 *
                 * Ищем стандартные обёртки:
                 *
                 * {"verses": ...}
                 * {"arguments": ...}
                 * {"parameters": ...}
                 * {"result": ...}
                 * {"data": ...}
                 */
                if (versesArrayNode.isObject()) {

                    // Главный ожидаемый случай.
                    JsonNode nestedVerses = versesArrayNode.get("verses");
                    if (nestedVerses != null && !nestedVerses.isNull()) {
                        versesArrayNode = nestedVerses;
                        continue;
                    }

                    // Иногда tool arguments дополнительно обёрнуты.
                    JsonNode arguments = versesArrayNode.get("arguments");
                    if (arguments != null && !arguments.isNull()) {
                        versesArrayNode = arguments;
                        continue;
                    }

                    JsonNode parameters = versesArrayNode.get("parameters");
                    if (parameters != null && !parameters.isNull()) {
                        versesArrayNode = parameters;
                        continue;
                    }

                    JsonNode result = versesArrayNode.get("result");
                    if (result != null && !result.isNull()) {
                        versesArrayNode = result;
                        continue;
                    }

                    JsonNode data = versesArrayNode.get("data");
                    if (data != null && !data.isNull()) {
                        versesArrayNode = data;
                        continue;
                    }

                    /*
                     * Если это сам объект одного стиха:
                     *
                     * {
                     *   "verseIndex": 0,
                     *   "textDevanagari": "...",
                     *   ...
                     * }
                     *
                     * превращаем его в массив из одного элемента.
                     */
                    if (versesArrayNode.has("verseIndex")) {
                        ArrayNode singleVerseArray = objectMapper.createArrayNode();
                        singleVerseArray.add(versesArrayNode);
                        versesArrayNode = singleVerseArray;
                        break;
                    }

                    log.warn("LLM returned JSON object, but no verses/arguments/result/data "
                                    + "field was found. Keys: {}",
                            java.util.stream.StreamSupport.stream(
                                            java.util.Spliterators.spliteratorUnknownSize(
                                                    versesArrayNode.fieldNames(), 0),
                                            false)
                                    .toList());

                    versesArrayNode = null;
                    break;
                }

                // Любой другой JSON type нам не подходит.
                log.warn("Unsupported LLM response JSON node type: {}",
                        versesArrayNode.getNodeType());

                versesArrayNode = null;
                break;
            }

        } catch (Exception e) {
            log.error("Failed to normalize LLM response", e);
            versesArrayNode = null;
        }

        if (versesArrayNode == null || !versesArrayNode.isArray()) {
            log.error("LLM did not return a usable verses array. Extracted node: {}",
                    versesArrayNode == null ? "null" : versesArrayNode.toString());
            return null;
        }

        if (log.isDebugEnabled()) {
            try {
                log.debug("Normalized verses array: {}",
                        objectMapper.writeValueAsString(versesArrayNode));
            } catch (Exception ignored) {
            }
        }

        return versesArrayNode;
    }
}
