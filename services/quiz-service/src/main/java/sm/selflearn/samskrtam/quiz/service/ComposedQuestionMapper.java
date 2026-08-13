package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.quest.declension.DeclensionMatchPayload;
import sm.selflearn.samskrtam.quiz.dto.ComposedQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionMatchRowDto;
import sm.selflearn.samskrtam.quiz.dto.QuestionOptionDto;
import sm.selflearn.samskrtam.quiz.dto.QuestItemDto;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure mapping of curriculum-composed questions to {@link SessionQuestion} (persistence)
 * and {@link QuestionDto} (response). Free of reactive/WebClient concerns, so the logic
 * is unit-testable without mocks.
 *
 * <p>Two rendering shapes:
 * <ul>
 *   <li>choice / free-text — options are correctAnswer + distractors, ids are
 *       deterministic (stable across resume);</li>
 *   <li>MATCHING — right-side labels are the distinct case+number labels from the payload
 *       (optionType MATCH_LABEL, with caseType/numberType carried in the option JSON),
 *       left-side rows are the word-form pairs (stable row id = payload pair id).</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ComposedQuestionMapper {

    private static final String ANSWER_MODE_MATCHING = "MATCHING";
    private static final String ANSWER_MODE_FREE_TEXT = "FREE_TEXT";

    private final ObjectMapper objectMapper;

    /**
     * Builds the rendered option list (correct + distractors, shuffled) with deterministic
     * ids so that resume rehydration reproduces the exact same options/ids. Persisted in
     * session_questions.options; the frontend submits {@code selectedOptionId}.
     */
    String buildOptionsJson(QuestItemDto item) {
        if (ANSWER_MODE_MATCHING.equals(item.answerMode())) {
            return buildMatchOptionsJson(item);
        }
        if (ANSWER_MODE_FREE_TEXT.equals(item.answerMode())) {
            // Free-text questions intentionally carry no options: the expected answer is
            // entered as text, not picked from a list. The correctAnswer must not leak
            // into the rendered options (otherwise the user would just click it).
            return "[]";
        }
        List<ObjectNode> options = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        if (item.correctAnswer() != null) {
            texts.add(item.correctAnswer());
        }
        if (item.distractors() != null) {
            texts.addAll(item.distractors());
        }
        Map<String, String> ruByText = ruVariantByText(item);
        Collections.shuffle(texts);
        for (String text : texts) {
            UUID optionId = UUID.nameUUIDFromBytes(
                    (item.id() + "|" + text).getBytes(StandardCharsets.UTF_8));
            ObjectNode option = objectMapper.createObjectNode();
            option.put("id", optionId.toString());
            option.put("text", text);
            String textRu = ruByText.get(text);
            if (textRu != null) {
                option.put("textRu", textRu);
            }
            options.add(option);
        }
        return arrayToJson(options);
    }

    /**
     * Russian variant lookup keyed by the canonical English option text, aligned by index
     * between {@code correctAnswer}/{@code distractors} and
     * {@code correctAnswerRu}/{@code distractorsRu}. Only the bilingual variants are mapped;
     * language-neutral option texts (word forms) have no Russian variant.
     */
    private Map<String, String> ruVariantByText(QuestItemDto item) {
        Map<String, String> ruByText = new LinkedHashMap<>();
        if (item.correctAnswer() != null && item.correctAnswerRu() != null) {
            ruByText.put(item.correctAnswer(), item.correctAnswerRu());
        }
        if (item.distractors() != null && item.distractorsRu() != null) {
            int size = Math.min(item.distractors().size(), item.distractorsRu().size());
            for (int i = 0; i < size; i++) {
                if (item.distractorsRu().get(i) != null) {
                    ruByText.put(item.distractors().get(i), item.distractorsRu().get(i));
                }
            }
        }
        return ruByText;
    }

    /**
     * Right-side MATCHING labels: the distinct case+number combinations across the payload
     * pairs. Each label carries caseType/numberType (needed for backend verification) and a
     * deterministic id.
     */
    private String buildMatchOptionsJson(QuestItemDto item) {
        DeclensionMatchPayload payload = parseMatchPayload(item);
        List<ObjectNode> options = new ArrayList<>();
        Map<String, DeclensionMatchPayload.DeclensionMatchPair> labels = new LinkedHashMap<>();
        for (DeclensionMatchPayload.DeclensionMatchPair pair : payload.pairs()) {
            labels.putIfAbsent(pair.caseType() + "|" + pair.numberType(), pair);
        }
        for (var labelPair : labels.values()) {
            String text = label(labelPair.caseType(), labelPair.numberType());
            UUID optionId = UUID.nameUUIDFromBytes(
                    (item.id() + "|L|" + labelPair.caseType() + "|" + labelPair.numberType())
                            .getBytes(StandardCharsets.UTF_8));
            ObjectNode option = objectMapper.createObjectNode();
            option.put("id", optionId.toString());
            option.put("text", text);
            option.put("caseType", labelPair.caseType());
            option.put("numberType", labelPair.numberType());
            options.add(option);
        }
        return arrayToJson(options);
    }

    private DeclensionMatchPayload parseMatchPayload(QuestItemDto item) {
        if (item.payload() == null) {
            throw new IllegalStateException("MATCHING item without payload: " + item.id());
        }
        try {
            return objectMapper.treeToValue(item.payload(), DeclensionMatchPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse MATCHING payload for item " + item.id(), e);
        }
    }

    private String label(String caseType, String numberType) {
        return caseType + " " + numberType;
    }

    String payloadToJson(com.fasterxml.jackson.databind.JsonNode payload) {
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize quest item payload", e);
        }
    }

    /** Maps curriculum answerMode to the frontend questionType contract. */
    String questionType(String answerMode) {
        if (answerMode == null) {
            return null;
        }
        return switch (answerMode) {
            case "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> "MULTIPLE_CHOICE";
            case ANSWER_MODE_MATCHING -> "MATCHING";
            default -> "FREE_TEXT"; // FREE_TEXT (and any other input mode)
        };
    }

    SessionQuestion toSessionQuestion(UUID sessionId, ComposedQuestionDto composed) {
        QuestItemDto item = composed.item();
        return SessionQuestion.builder()
                .id(null)
                .sessionId(sessionId)
                .questionId(item.id())
                .questionNumber(composed.questionNumber())
                .text(item.prompt())
                .textRu(item.promptRu())
                .itemType(item.itemType())
                .answerMode(item.answerMode())
                .correctAnswer(item.correctAnswer())
                .options(Json.of(buildOptionsJson(item)))
                .payload(item.payload() == null ? null : Json.of(payloadToJson(item.payload())))
                .topicCode(composed.topicCode())
                .progressTag(composed.progressTag())
                .questionType(questionType(item.answerMode()))
                .build();
    }

    QuestionDto toQuestionDto(SessionQuestion q) {
        QuestionDto.QuestionDtoBuilder builder = QuestionDto.builder()
                .id(q.getQuestionId())
                .questionNumber(q.getQuestionNumber())
                .text(q.getText())
                .textRu(q.getTextRu())
                .questionType(q.getQuestionType())
                .answerMode(q.getAnswerMode())
                .multiSelect(false)
                .options(parseOptions(q.getOptions()));
        if ("MATCHING".equals(q.getQuestionType())) {
            builder.matchRows(parseMatchRows(q));
        }
        return builder.build();
    }

    private List<QuestionMatchRowDto> parseMatchRows(SessionQuestion q) {
        if (q.getPayload() == null || q.getPayload().asString() == null) {
            return List.of();
        }
        try {
            DeclensionMatchPayload payload = objectMapper.treeToValue(
                    objectMapper.readTree(q.getPayload().asString()), DeclensionMatchPayload.class);
            List<QuestionMatchRowDto> rows = new ArrayList<>();
            for (DeclensionMatchPayload.DeclensionMatchPair pair : payload.pairs()) {
                rows.add(new QuestionMatchRowDto(
                        UUID.fromString(pair.pairId()),
                        pair.wordFormIast(),
                        pair.wordFormDevanagari(),
                        pair.caseType(),
                        pair.numberType()));
            }
            return rows;
        } catch (Exception e) {
            return List.of();
        }
    }

    List<QuestionOptionDto> parseOptions(Json options) {
        if (options == null || options.asString() == null || options.asString().isBlank()) {
            return List.of();
        }
        try {
            var array = (ArrayNode) objectMapper.readTree(options.asString());
            List<QuestionOptionDto> result = new ArrayList<>();
            array.forEach(node -> result.add(QuestionOptionDto.builder()
                    .id(UUID.fromString(node.get("id").asText()))
                    .optionType(node.has("caseType") ? "MATCH_LABEL" : "FORM")
                    .formIast(node.get("text").asText())
                    .textRu(node.has("textRu") ? node.get("textRu").asText() : null)
                    .caseType(node.has("caseType") ? node.get("caseType").asText() : null)
                    .numberType(node.has("numberType") ? node.get("numberType").asText() : null)
                    .build()));
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    /** The option text for a given option id, or {@code null} when not found/absent. */
    String resolveOptionText(Json options, UUID optionId) {
        return parseOptions(options).stream()
                .filter(o -> o.getId().equals(optionId))
                .map(QuestionOptionDto::getFormIast)
                .findFirst()
                .orElse(null);
    }

    /** The id of the option whose text equals the correct answer, or {@code null}. */
    UUID findCorrectOptionId(Json options, String correctAnswer) {
        if (correctAnswer == null) {
            return null;
        }
        return parseOptions(options).stream()
                .filter(o -> correctAnswer.equals(o.getFormIast()))
                .map(QuestionOptionDto::getId)
                .findFirst()
                .orElse(null);
    }

    /**
     * For a MATCHING question, the mapping optionId → (caseType, numberType) used by the
     * answer verifier. Empty for non-matching questions.
     */
    Map<UUID, String[]> parseMatchLabelMap(Json options) {
        Map<UUID, String[]> map = new LinkedHashMap<>();
        for (QuestionOptionDto o : parseOptions(options)) {
            if (o.getCaseType() != null && o.getNumberType() != null) {
                map.put(o.getId(), new String[]{o.getCaseType(), o.getNumberType()});
            }
        }
        return map;
    }

    /**
     * For a MATCHING question, the reference mapping pairId → (caseType, numberType)
     * read from the persisted payload. Empty when the payload is missing/illegal.
     */
    Map<UUID, String[]> parseMatchPairMap(Json payload) {
        Map<UUID, String[]> map = new LinkedHashMap<>();
        if (payload == null || payload.asString() == null) {
            return map;
        }
        try {
            DeclensionMatchPayload parsed = objectMapper.treeToValue(
                    objectMapper.readTree(payload.asString()), DeclensionMatchPayload.class);
            for (DeclensionMatchPayload.DeclensionMatchPair pair : parsed.pairs()) {
                map.put(UUID.fromString(pair.pairId()), new String[]{pair.caseType(), pair.numberType()});
            }
        } catch (Exception ignored) {
            // malformed payload → treated as no reference pairs → answer counted incorrect
        }
        return map;
    }

    private String arrayToJson(List<ObjectNode> nodes) {
        ArrayNode array = objectMapper.createArrayNode();
        nodes.forEach(array::add);
        try {
            return objectMapper.writeValueAsString(array);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize options", e);
        }
    }
}
