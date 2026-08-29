package sm.selflearn.samskrtam.curriculum.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.curriculum.dto.SandhiRuleDto;
import sm.selflearn.samskrtam.curriculum.dto.SandhiRulesResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SandhiRuleService {

    private final ObjectMapper objectMapper;
    private List<SandhiRuleDto> allRules = List.of();
    private Map<String, String> categoryGlossary = Map.of();

    /**
     * Maps a curriculum topic code to the JSON section names whose rules
     * should be displayed on that topic's page.
     */
    private static final Map<String, List<String>> TOPIC_TO_SECTIONS = Map.of(
            "sandhi-vowels-external", List.of("external_vowels"),
            "sandhi-consonants", List.of("external_nasals", "external_plosives"),
            "sandhi-visarga", List.of("external_visarga")
    );

    @PostConstruct
    void loadRules() {
        try {
            ClassPathResource resource = new ClassPathResource("sandhi/sandhi-rules.json");
            try (InputStream is = resource.getInputStream()) {
                var root = objectMapper.readTree(is);
                var rulesNode = root.get("rules");
                if (rulesNode != null && rulesNode.isArray()) {
                    List<SandhiRuleDto> loaded = new ArrayList<>();
                    for (var node : rulesNode) {
                        try {
                            SandhiRuleDto rule = objectMapper.treeToValue(node, SandhiRuleDto.class);
                            loaded.add(rule);
                        } catch (Exception e) {
                            log.warn("Failed to parse sandhi rule node: {}", e.getMessage());
                        }
                    }
                    allRules = List.copyOf(loaded);
                    log.info("Loaded {} sandhi rules from sandhi/sandhi-rules.json", allRules.size());
                } else {
                    log.error("No 'rules' array found in sandhi-rules.json");
                }

                var glossaryNode = root.get("category_glossary");
                if (glossaryNode != null && glossaryNode.isObject()) {
                    Map<String, String> glossary = new HashMap<>();
                    glossaryNode.fieldNames().forEachRemaining(key -> {
                        var value = glossaryNode.get(key);
                        if (value != null && value.isTextual()) {
                            glossary.put(key, value.textValue());
                        }
                    });
                    categoryGlossary = Map.copyOf(glossary);
                    log.info("Loaded {} category glossary entries", categoryGlossary.size());
                }
            }
        } catch (IOException e) {
            log.error("Failed to load sandhi rules JSON", e);
        }
    }

    public SandhiRulesResponse getAllRules() {
        return new SandhiRulesResponse(null, "Все правила сандхи", allRules, categoryGlossary);
    }

    public SandhiRulesResponse getRulesForTopic(String topicCode) {
        List<String> sections = TOPIC_TO_SECTIONS.getOrDefault(topicCode, List.of());

        List<SandhiRuleDto> filtered = allRules.stream()
                .filter(r -> sections.contains(r.section()))
                .toList();

        return new SandhiRulesResponse(topicCode, resolveTitle(topicCode), filtered, categoryGlossary);
    }

    private String resolveTitle(String topicCode) {
        return switch (topicCode) {
            case "sandhi-vowels-external" -> "Внешние сандхи: гласные";
            case "sandhi-consonants" -> "Внешние сандхи: согласные";
            case "sandhi-visarga" -> "Внешние сандхи: висарга";
            default -> topicCode;
        };
    }
}