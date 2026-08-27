package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.r2dbc.postgresql.codec.Json;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.quest.AnswerMode;
import sm.selflearn.samskrtam.quest.HighlightToken;
import sm.selflearn.samskrtam.quest.declension.DeclensionMatchPayload;
import sm.selflearn.samskrtam.quest.conjugation.ConjugationMatchPayload;
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

    private final ObjectMapper objectMapper;

    /**
     * Builds the rendered option list (correct + distractors, shuffled) with deterministic
     * ids so that resume rehydration reproduces the exact same options/ids. Persisted in
     * session_questions.options; the frontend submits {@code selectedOptionId}.
     */
    String buildOptionsJson(QuestItemDto item) {
        if (item.answerMode() == AnswerMode.MATCHING) {
            return buildMatchOptionsJson(item);
        }
        if (item.answerMode() == AnswerMode.FREE_TEXT) {
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
     * Right-side MATCHING labels: for conjugation items, the distinct
     * person+numberType+voice combinations; for declension, caseType+numberType.
     */
    private String buildMatchOptionsJson(QuestItemDto item) {
        if (isConjugationItem(item)) {
            return buildConjugationMatchOptions(item);
        }
        return buildDeclensionMatchOptions(item);
    }

    private String buildDeclensionMatchOptions(QuestItemDto item) {
        DeclensionMatchPayload payload = parseDeclensionMatchPayload(item);
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

    private String buildConjugationMatchOptions(QuestItemDto item) {
        ConjugationMatchPayload payload = parseConjugationMatchPayload(item);
        List<ObjectNode> options = new ArrayList<>();
        Map<String, ConjugationMatchPayload.ConjugationMatchPair> labels = new LinkedHashMap<>();
        for (ConjugationMatchPayload.ConjugationMatchPair pair : payload.pairs()) {
            labels.putIfAbsent(pair.person() + "|" + pair.numberType() + "|" + pair.voice(), pair);
        }
        for (var labelPair : labels.values()) {
            String text = personLabelEn(labelPair.person()) + " " + numberLabel(labelPair.numberType())
                    + " " + voiceLabelEn(labelPair.voice());
            UUID optionId = UUID.nameUUIDFromBytes(
                    (item.id() + "|L|" + labelPair.person() + "|" + labelPair.numberType() + "|" + labelPair.voice())
                            .getBytes(StandardCharsets.UTF_8));
            ObjectNode option = objectMapper.createObjectNode();
            option.put("id", optionId.toString());
            option.put("text", text);
            option.put("person", labelPair.person());
            option.put("numberType", labelPair.numberType());
            option.put("voice", labelPair.voice());
            options.add(option);
        }
        return arrayToJson(options);
    }

    private boolean isConjugationItem(QuestItemDto item) {
        return item.itemType() != null && item.itemType().startsWith("CONJUGATION_");
    }

    private DeclensionMatchPayload parseDeclensionMatchPayload(QuestItemDto item) {
        if (item.payload() == null) {
            throw new IllegalStateException("MATCHING item without payload: " + item.id());
        }
        try {
            return objectMapper.treeToValue(item.payload(), DeclensionMatchPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse DECLENSION_MATCH payload for item " + item.id(), e);
        }
    }

    private ConjugationMatchPayload parseConjugationMatchPayload(QuestItemDto item) {
        if (item.payload() == null) {
            throw new IllegalStateException("MATCHING item without payload: " + item.id());
        }
        try {
            return objectMapper.treeToValue(item.payload(), ConjugationMatchPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse CONJUGATION_MATCH payload for item " + item.id(), e);
        }
    }

    private String label(String caseType, String numberType) {
        return caseType + " " + numberType;
    }

    private static String personLabelEn(int person) {
        return switch (person) { case 1 -> "1st"; case 2 -> "2nd"; default -> "3rd"; };
    }

    private static String numberLabel(String numberType) {
        try {
            return sm.selflearn.samskrtam.morphology.NumberType.valueOf(numberType).getEnName();
        } catch (IllegalArgumentException e) {
            return numberType;
        }
    }

    private static String voiceLabelEn(String voice) {
        if ("PARASMAIPADA".equals(voice)) return "active";
        if ("ATMANEPADA".equals(voice)) return "middle";
        return voice;
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
    String questionType(AnswerMode answerMode) {
        if (answerMode == null) {
            return null;
        }
        return switch (answerMode) {
            case SINGLE_CHOICE -> "MULTIPLE_CHOICE";
            case MATCHING -> "MATCHING";
            case FREE_TEXT, MULTI_SELECT, SPAN_SELECT -> "FREE_TEXT";
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
                .highlights(parseHighlights(q.getPayload()))
                .options(parseOptions(q.getOptions()));
        if (q.getAnswerMode() == AnswerMode.MATCHING) {
            builder.matchRows(parseMatchRows(q));
        }
        return builder.build();
    }

    /**
     * Highlight tokens embedded in the curriculum payload (bilingual prompt words).
     * Empty when the payload is missing/illegal.
     */
    private List<HighlightToken> parseHighlights(Json payload) {
        if (payload == null || payload.asString() == null) {
            return List.of();
        }
        try {
            var node = objectMapper.readTree(payload.asString());
            JsonNode highlights = node.get("highlights");
            if (highlights == null || !highlights.isArray()) {
                return List.of();
            }
            List<HighlightToken> tokens = new ArrayList<>();
            for (JsonNode token : highlights) {
                tokens.add(new HighlightToken(
                        token.hasNonNull("text") ? token.get("text").asText() : null,
                        token.hasNonNull("textRu") ? token.get("textRu").asText() : null));
            }
            return tokens;
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<QuestionMatchRowDto> parseMatchRows(SessionQuestion q) {
        if (q.getPayload() == null || q.getPayload().asString() == null) {
            return List.of();
        }
        try {
            if (q.getItemType() != null && q.getItemType().startsWith("CONJUGATION_")) {
                return parseConjugationMatchRows(q);
            }
            return parseDeclensionMatchRows(q);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<QuestionMatchRowDto> parseDeclensionMatchRows(SessionQuestion q) throws Exception {
        DeclensionMatchPayload payload = objectMapper.treeToValue(
                objectMapper.readTree(q.getPayload().asString()), DeclensionMatchPayload.class);
        List<QuestionMatchRowDto> rows = new ArrayList<>();
        for (DeclensionMatchPayload.DeclensionMatchPair pair : payload.pairs()) {
            rows.add(new QuestionMatchRowDto(
                    UUID.fromString(pair.pairId()),
                    pair.wordFormIast(),
                    pair.wordFormDevanagari(),
                    pair.caseType(),
                    pair.numberType(),
                    null, null));
        }
        return rows;
    }

    private List<QuestionMatchRowDto> parseConjugationMatchRows(SessionQuestion q) throws Exception {
        ConjugationMatchPayload payload = objectMapper.treeToValue(
                objectMapper.readTree(q.getPayload().asString()), ConjugationMatchPayload.class);
        List<QuestionMatchRowDto> rows = new ArrayList<>();
        for (ConjugationMatchPayload.ConjugationMatchPair pair : payload.pairs()) {
            rows.add(new QuestionMatchRowDto(
                    UUID.fromString(pair.pairId()),
                    pair.wordFormIast(),
                    pair.wordFormDevanagari(),
                    null, null,
                    pair.person(),
                    pair.voice()));
        }
        return rows;
    }

    List<QuestionOptionDto> parseOptions(Json options) {
        if (options == null || options.asString() == null || options.asString().isBlank()) {
            return List.of();
        }
        try {
            var array = (ArrayNode) objectMapper.readTree(options.asString());
            List<QuestionOptionDto> result = new ArrayList<>();
            array.forEach(node -> {
                var builder = QuestionOptionDto.builder()
                        .id(UUID.fromString(node.get("id").asText()))
                        .optionType(node.has("caseType") ? "MATCH_LABEL" : node.has("person") ? "MATCH_LABEL_CONJ" : "FORM")
                        .formIast(node.get("text").asText())
                        .textRu(node.has("textRu") ? node.get("textRu").asText() : null)
                        .caseType(node.has("caseType") ? node.get("caseType").asText() : null)
                        .numberType(node.has("numberType") ? node.get("numberType").asText() : null);
                if (node.has("person")) {
                    builder.person(node.get("person").asInt());
                }
                if (node.has("voice")) {
                    builder.voice(node.get("voice").asText());
                }
                result.add(builder.build());
            });
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
     * For a MATCHING question, the mapping optionId → (caseType, numberType) or
     * (person, numberType, voice) used by the answer verifier.
     * Empty for non-matching questions.
     */
    Map<UUID, String[]> parseMatchLabelMap(Json options) {
        Map<UUID, String[]> map = new LinkedHashMap<>();
        for (QuestionOptionDto o : parseOptions(options)) {
            if (o.getCaseType() != null && o.getNumberType() != null) {
                map.put(o.getId(), new String[]{o.getCaseType(), o.getNumberType()});
            } else if (o.getNumberType() != null && o.getVoice() != null && o.getPerson() != null) {
                map.put(o.getId(), new String[]{String.valueOf(o.getPerson()), o.getNumberType(), o.getVoice()});
            }
        }
        return map;
    }

    /**
     * For a MATCHING question, the reference mapping pairId → (caseType, numberType)
     * or (person, numberType, voice) read from the persisted payload.
     */
    Map<UUID, String[]> parseMatchPairMap(Json payload) {
        Map<UUID, String[]> map = new LinkedHashMap<>();
        if (payload == null || payload.asString() == null) {
            return map;
        }
        try {
            var root = objectMapper.readTree(payload.asString());
            // Try conjugation payload first
            if (root.has("pairs") && root.get("pairs").isArray() && root.get("pairs").size() > 0
                    && root.get("pairs").get(0).has("person")) {
                ConjugationMatchPayload parsed = objectMapper.treeToValue(root, ConjugationMatchPayload.class);
                for (ConjugationMatchPayload.ConjugationMatchPair pair : parsed.pairs()) {
                    map.put(UUID.fromString(pair.pairId()),
                            new String[]{String.valueOf(pair.person()), pair.numberType(), pair.voice()});
                }
            } else {
                DeclensionMatchPayload parsed = objectMapper.treeToValue(root, DeclensionMatchPayload.class);
                for (DeclensionMatchPayload.DeclensionMatchPair pair : parsed.pairs()) {
                    map.put(UUID.fromString(pair.pairId()),
                            new String[]{pair.caseType(), pair.numberType()});
                }
            }
        } catch (Exception ignored) {
            // malformed payload → treated as no reference pairs
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
