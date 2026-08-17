package sm.selflearn.samskrtam.quiz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.quest.AnswerMode;
import sm.selflearn.samskrtam.quest.declension.DeclensionMatchPayload;
import sm.selflearn.samskrtam.quiz.dto.ComposedQuestionDto;
import sm.selflearn.samskrtam.quiz.dto.QuestItemDto;
import sm.selflearn.samskrtam.quiz.model.SessionQuestion;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ComposedQuestionMapperTest {

    private ComposedQuestionMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mapper = new ComposedQuestionMapper(objectMapper);
    }

    @Test
    void mapsComposedQuestionToSessionQuestionWithOptions() throws Exception {
        UUID qid = UUID.randomUUID();
        QuestItemDto item = new QuestItemDto(
                qid, "DECLENSION_FORM_CHOICE", AnswerMode.SINGLE_CHOICE,
                "Choose the acc. sg. of nara", null, "B", null, List.of("A", "C"), null, null, null);
        ComposedQuestionDto composed = new ComposedQuestionDto(3, "a-stem-masc", item, null);

        SessionQuestion q = mapper.toSessionQuestion(UUID.randomUUID(), composed);

        assertThat(q.getQuestionId()).isEqualTo(qid);
        assertThat(q.getQuestionNumber()).isEqualTo(3);
        assertThat(q.getItemType()).isEqualTo("DECLENSION_FORM_CHOICE");
        assertThat(q.getAnswerMode()).isEqualTo(AnswerMode.SINGLE_CHOICE);
        assertThat(q.getCorrectAnswer()).isEqualTo("B");
        assertThat(q.getTopicCode()).isEqualTo("a-stem-masc");
        assertThat(q.getQuestionType()).isEqualTo("MULTIPLE_CHOICE");

        var array = (com.fasterxml.jackson.databind.node.ArrayNode)
                objectMapper.readTree(q.getOptions().asString());
        assertThat(array).hasSize(3); // correct + 2 distractors
        List<String> texts = new java.util.ArrayList<>();
        array.forEach(n -> texts.add(n.get("text").asText()));
        assertThat(texts).containsExactlyInAnyOrder("B", "A", "C");
    }

    @Test
    void bilingualItemCarriesRussianPromptAndOptionText() throws Exception {
        UUID qid = UUID.randomUUID();
        QuestItemDto item = new QuestItemDto(
                qid, "CASE_RECOGNITION", AnswerMode.SINGLE_CHOICE,
                "Identify the case and number of 'naram'.", "RU prompt",
                "Accusative Singular", "RU accusative singular",
                List.of("Nominative Singular", "Locative Plural"),
                List.of("RU nominative singular", "RU locative plural"),
                null, null);
        SessionQuestion q = mapper.toSessionQuestion(
                UUID.randomUUID(), new ComposedQuestionDto(1, "a-stem", item, null));

        assertThat(q.getText()).isEqualTo("Identify the case and number of 'naram'.");
        assertThat(q.getTextRu()).isEqualTo("RU prompt");

        var array = (com.fasterxml.jackson.databind.node.ArrayNode)
                objectMapper.readTree(q.getOptions().asString());
        assertThat(array).hasSize(3);
        for (var node : array) {
            assertThat(node.get("text").asText()).isNotBlank();
            assertThat(node.get("textRu").asText()).isNotBlank();
        }

        var dto = mapper.toQuestionDto(q);
        assertThat(dto.getTextRu()).isEqualTo("RU prompt");
        assertThat(dto.getOptions()).extracting("textRu").doesNotContainNull();
    }

    @Test
    void optionIdsAreDeterministicPerText() throws Exception {
        UUID qid = UUID.randomUUID();
        QuestItemDto item = new QuestItemDto(qid, "TYPE", AnswerMode.SINGLE_CHOICE, "p", null, "B", null, List.of("A", "C"), null, null, null);
        ComposedQuestionDto composed = new ComposedQuestionDto(1, "t", item, null);

        SessionQuestion first = mapper.toSessionQuestion(UUID.randomUUID(), composed);
        SessionQuestion second = mapper.toSessionQuestion(UUID.randomUUID(), composed);

        // Option ORDER is shuffled at compose time, but the id of a given option text is
        // stable, so a resume rehydrates the same options/ids.
        assertThat(textToId(first)).containsExactlyInAnyOrderEntriesOf(textToId(second));
    }

    private java.util.Map<String, String> textToId(SessionQuestion q) throws Exception {
        var array = (com.fasterxml.jackson.databind.node.ArrayNode)
                objectMapper.readTree(q.getOptions().asString());
        java.util.Map<String, String> map = new java.util.HashMap<>();
        array.forEach(n -> map.put(n.get("text").asText(), n.get("id").asText()));
        return map;
    }

    @Test
    void freeTextAnswerModeMapsToFreeTextQuestionType() {
        QuestItemDto item = new QuestItemDto(UUID.randomUUID(), "n", AnswerMode.FREE_TEXT, "p", null, "ans", null, List.of(), null, null, null);
        assertThat(mapper.questionType(item.answerMode())).isEqualTo("FREE_TEXT");
    }

    @Test
    void freeTextItemBuildsNoOptions() throws Exception {
        QuestItemDto item = new QuestItemDto(UUID.randomUUID(), "n", AnswerMode.FREE_TEXT, "p", null, "ans", null, List.of(), null, null, null);
        String json = mapper.buildOptionsJson(item);
        var array = (com.fasterxml.jackson.databind.node.ArrayNode) objectMapper.readTree(json);
        assertThat(array).isEmpty();
    }

    @Test
    void questionDtoOptionsPreserveStoredIds() {
        UUID qid = UUID.randomUUID();
        QuestItemDto item = new QuestItemDto(qid, "n", AnswerMode.SINGLE_CHOICE, "p", null, "B", null, List.of("A"), null, null, null);
        SessionQuestion q = mapper.toSessionQuestion(UUID.randomUUID(), new ComposedQuestionDto(1, "t", item, null));

        var dto = mapper.toQuestionDto(q);
        assertThat(dto.getOptions()).hasSize(2);
        assertThat(dto.getOptions().get(0).getFormIast()).isNotNull();
    }

    @Test
    void matchingRendersLabelOptionsAndRows() throws Exception {
        UUID qid = UUID.randomUUID();
        String pair1 = UUID.randomUUID().toString();
        String pair2 = UUID.randomUUID().toString();
        DeclensionMatchPayload payload = new DeclensionMatchPayload(
                "nara", "a-stem",
                List.of(
                        new DeclensionMatchPayload.DeclensionMatchPair(pair1, "naram", "नरम्", "ACCUSATIVE", "SINGULAR"),
                        new DeclensionMatchPayload.DeclensionMatchPair(pair2, "nare", "नरे", "LOCATIVE", "SINGULAR")));
        QuestItemDto item = new QuestItemDto(
                qid, "DECLENSION_MATCH", AnswerMode.MATCHING, "Match", null, null, null, List.of(), null,
                objectMapper.valueToTree(payload), null);
        SessionQuestion q = mapper.toSessionQuestion(UUID.randomUUID(), new ComposedQuestionDto(1, "a-stem", item, null));

        assertThat(q.getQuestionType()).isEqualTo("MATCHING");
        assertThat(q.getCorrectAnswer()).isNull();

        var labels = (com.fasterxml.jackson.databind.node.ArrayNode)
                objectMapper.readTree(q.getOptions().asString());
        assertThat(labels).hasSize(2); // two distinct case×number labels
        assertThat(labels.get(0).get("caseType").asText()).isNotBlank();
        assertThat(labels.get(0).get("numberType").asText()).isNotBlank();
        assertThat(labels.get(0).get("id").asText()).isNotBlank();

        var dto = mapper.toQuestionDto(q);
        assertThat(dto.getQuestionType()).isEqualTo("MATCHING");
        assertThat(dto.getMatchRows()).hasSize(2);
        assertThat(dto.getMatchRows().get(0).wordFormIast()).isEqualTo("naram");
        assertThat(dto.getMatchRows().get(0).caseType()).isEqualTo("ACCUSATIVE");
        assertThat(dto.getOptions()).extracting("optionType").containsOnly("MATCH_LABEL");
        assertThat(dto.getOptions()).extracting("caseType").contains("ACCUSATIVE", "LOCATIVE");

        var pairMap = mapper.parseMatchPairMap(q.getPayload());
        assertThat(pairMap).hasSize(2);
        assertThat(pairMap.get(UUID.fromString(pair1)))
                .containsExactly("ACCUSATIVE", "SINGULAR");
        assertThat(pairMap.get(UUID.fromString(pair2)))
                .containsExactly("LOCATIVE", "SINGULAR");
    }
}