package sm.selflearn.samskrtam.curriculum.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SandhiRuleDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserialize_mapsDependsOnSnakeCase_andSerializesCamelCase() throws Exception {
        String json = """
                {
                  "number": 8,
                  "section": "internal_consonants",
                  "applicability": "internal",
                  "text": "text",
                  "example": null,
                  "reference": "W 150b",
                  "depends_on": [9, 31],
                  "depends_on_note": "note"
                }
                """;

        JsonNode node = objectMapper.readTree(json);
        SandhiRuleDto rule = objectMapper.treeToValue(node, SandhiRuleDto.class);

        assertThat(rule.number()).isEqualTo(8);
        assertThat(rule.dependsOn()).containsExactly(9, 31);
        assertThat(rule.dependsOnNote()).isEqualTo("note");

        JsonNode serialized = objectMapper.valueToTree(rule);
        assertThat(serialized.has("dependsOn")).isTrue();
        assertThat(serialized.has("depends_on")).isFalse();
        assertThat(serialized.get("dependsOn").isArray()).isTrue();
        assertThat(serialized.get("dependsOn").get(0).asInt()).isEqualTo(9);
        assertThat(serialized.get("dependsOn").get(1).asInt()).isEqualTo(31);
    }

    @Test
    void deserialize_ruleWithoutDependsOn_leavesFieldsNull() throws Exception {
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
        assertThat(rule.dependsOn()).isNull();
        assertThat(rule.dependsOnNote()).isNull();
    }
}
