package sm.selflearn.samskrtam.curriculum.questitem.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sm.selflearn.samskrtam.curriculum.questitem.QuestItem;
import sm.selflearn.samskrtam.curriculum.questitem.dto.QuestItemDto;
import sm.selflearn.samskrtam.quest.AnswerMode;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class QuestItemMapperTest {

    private ObjectMapper objectMapper;
    private QuestItemMapper mapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mapper = new QuestItemMapperImpl();
        org.springframework.test.util.ReflectionTestUtils.setField(
                mapper, "questItemJsonSupport", new QuestItemJsonSupport(objectMapper));
    }

    @Test
    void toDto_matching_suppressesCorrectAnswerInDto() {
        QuestItem item = item(AnswerMode.MATCHING, "should-not-leak", "[]",
                "[{\"pairId\":\"a\",\"wordFormIast\":\"agninā\",\"caseType\":\"INSTRUMENTAL\",\"numberType\":\"SINGULAR\"}]");

        QuestItemDto dto = mapper.toDto(item);

        assertThat(dto.correctAnswer()).isNull();
        assertThat(dto.answerMode()).isEqualTo(AnswerMode.MATCHING);
        assertThat(dto.distractors()).isEmpty();
        assertThat(dto.payload()).isNotNull();
    }

    @Test
    void serializedJson_matching_omitsCorrectAnswerField() throws Exception {
        QuestItem item = item(AnswerMode.MATCHING, "should-not-leak", "[]", "{\"x\":1}");

        String json = objectMapper.writeValueAsString(mapper.toDto(item));

        assertThat(json).doesNotContain("correctAnswer");
    }

    @Test
    void serializedJson_freeText_keepsCorrectAnswerField() throws Exception {
        QuestItem item = item(AnswerMode.FREE_TEXT, "naraḥ", "[]", "{\"a\":1}");

        String json = objectMapper.writeValueAsString(mapper.toDto(item));

        assertThat(json).contains("\"correctAnswer\":\"naraḥ\"");
    }

    @Test
    void toDto_deserializesDistractorsList() {
        QuestItem item = item(AnswerMode.SINGLE_CHOICE, "naraḥ", "[\"naram\",\"narena\",\"narāya\"]", "{\"a\":1}");

        QuestItemDto dto = mapper.toDto(item);

        assertThat(dto.distractors()).containsExactly("naram", "narena", "narāya");
        assertThat(dto.correctAnswer()).isEqualTo("naraḥ");
    }

    @Test
    void toDto_mapsProgressTag() throws Exception {
        QuestItem item = item(AnswerMode.FREE_TEXT, "naraḥ", "[]", "{\"a\":1}");
        item.setProgressTag("NOMINATIVE|SINGULAR|MASCULINE");

        QuestItemDto dto = mapper.toDto(item);
        String json = objectMapper.writeValueAsString(dto);

        assertThat(dto.progressTag()).isEqualTo("NOMINATIVE|SINGULAR|MASCULINE");
        assertThat(json).contains("\"progressTag\":\"NOMINATIVE|SINGULAR|MASCULINE\"");
    }

    @Test
    void toDto_nullProgressTag_isOmittedFromJson() throws Exception {
        QuestItem item = item(AnswerMode.FREE_TEXT, "naraḥ", "[]", "{\"a\":1}");

        String json = objectMapper.writeValueAsString(mapper.toDto(item));

        assertThat(json).doesNotContain("progressTag");
    }

    @Test
    void toDto_mapsQuestPattern() throws Exception {
        QuestItem item = item(AnswerMode.FREE_TEXT, "naraḥ", "[]", "{\"a\":1}");
        item.setQuestPattern("nom-form");

        QuestItemDto dto = mapper.toDto(item);
        String json = objectMapper.writeValueAsString(dto);

        assertThat(dto.questPattern()).isEqualTo("nom-form");
        assertThat(json).contains("\"questPattern\":\"nom-form\"");
    }

    @Test
    void toDto_nullQuestPattern_isOmittedFromJson() throws Exception {
        QuestItem item = item(AnswerMode.FREE_TEXT, "naraḥ", "[]", "{\"a\":1}");

        String json = objectMapper.writeValueAsString(mapper.toDto(item));

        assertThat(json).doesNotContain("questPattern");
    }

    private static QuestItem item(AnswerMode answerMode, String correctAnswer,
                                  String distractorsJson, String payloadJson) {
        QuestItem item = new QuestItem();
        item.setId(UUID.randomUUID());
        item.setItemType("DECLENSION_FORM");
        item.setAnswerMode(answerMode);
        item.setPrompt("prompt");
        item.setCorrectAnswer(correctAnswer);
        item.setDistractors(distractorsJson);
        item.setPayload(payloadJson);
        return item;
    }
}