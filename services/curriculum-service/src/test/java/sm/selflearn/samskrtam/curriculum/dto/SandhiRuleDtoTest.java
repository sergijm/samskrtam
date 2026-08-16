package sm.selflearn.samskrtam.curriculum.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SandhiRuleDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserialize_mapsNewFieldsSnakeCase_andSerializesCamelCase() throws Exception {
        String json = """
                {
                  "number": 8,
                  "section": "internal_consonants",
                  "applicability": "internal",
                  "text": "text",
                  "example": null,
                  "reference": "W 150b",
                  "supersedes": [9, 31],
                  "default_for": [5],
                  "applies_with": [42]
                }
                """;

        JsonNode node = objectMapper.readTree(json);
        SandhiRuleDto rule = objectMapper.treeToValue(node, SandhiRuleDto.class);

        assertThat(rule.number()).isEqualTo(8);
        assertThat(rule.supersedes()).containsExactly(9, 31);
        assertThat(rule.defaultFor()).containsExactly(5);
        assertThat(rule.appliesWith()).containsExactly(42);

        JsonNode serialized = objectMapper.valueToTree(rule);
        assertThat(serialized.has("supersedes")).isTrue();
        assertThat(serialized.get("supersedes").isArray()).isTrue();
        assertThat(serialized.get("supersedes").get(0).asInt()).isEqualTo(9);
    }

    @Test
    void deserialize_ruleWithoutNewFields_leavesFieldsNull() throws Exception {
        String json = """
                {
                  "number": 1,
                  "section": "internal_vowels",
                  "applicability": "internal",
                  "text": "text",
                  "example": null,
                  "reference": "W 127"
                }
                """;

        SandhiRuleDto rule = objectMapper.treeToValue(objectMapper.readTree(json), SandhiRuleDto.class);

        assertThat(rule.number()).isEqualTo(1);
        assertThat(rule.supersedes()).isNull();
        assertThat(rule.defaultFor()).isNull();
        assertThat(rule.appliesWith()).isNull();
    }
}
