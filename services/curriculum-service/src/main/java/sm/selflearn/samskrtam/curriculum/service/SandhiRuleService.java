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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SandhiRuleService {

    private final ObjectMapper objectMapper;
    private List<SandhiRuleDto> allRules = List.of();

    /**
     * Maps a curriculum topic code to the JSON section names whose rules
     * should be displayed on that topic's page.
     */
    private static final Map<String, List<String>> TOPIC_TO_SECTIONS = Map.of(
            "sandhi-vowels-external", List.of("external_vowels"),
            "sandhi-consonants", List.of("external_nasals", "external_plosives"),
            "sandhi-visarga", List.of("external_visarga"),
            "sandhi-vowels-internal", List.of("internal_vowels"),
            "sandhi-consonants-internal", List.of("internal_consonants", "internal_consonants_h_final", "internal_consonants_clusters")
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
            }
        } catch (IOException e) {
            log.error("Failed to load sandhi rules JSON", e);
        }
    }

    public SandhiRulesResponse getRulesForTopic(String topicCode) {
        List<String> sections = TOPIC_TO_SECTIONS.getOrDefault(topicCode, List.of());

        List<SandhiRuleDto> filtered = allRules.stream()
                .filter(r -> sections.contains(r.section()))
                .toList();

        return new SandhiRulesResponse(topicCode, resolveTitle(topicCode), filtered);
    }

    public SandhiRulesResponse getRulesByNumbers(List<Integer> numbers) {
        Map<Integer, SandhiRuleDto> byNumber = allRules.stream()
                .collect(Collectors.toMap(SandhiRuleDto::number, Function.identity(), (a, b) -> a));

        Set<Integer> include = new LinkedHashSet<>(numbers);

        /*
         * Expand each requested rule with the rules it depends on and, transitively,
         * the dependencies of those rules, so the audience sees the full chain.
         */
        Deque<Integer> queue = new ArrayDeque<>(include);
        while (!queue.isEmpty()) {
            SandhiRuleDto rule = byNumber.get(queue.poll());
            if (rule == null || rule.dependson() == null) {
                continue;
            }
            for (Integer dep : rule.dependson()) {
                if (byNumber.containsKey(dep) && include.add(dep)) {
                    queue.add(dep);
                }
            }
        }

        List<SandhiRuleDto> filtered = include.stream()
                .filter(byNumber::containsKey)
                .sorted()
                .map(byNumber::get)
                .toList();

        return new SandhiRulesResponse("by-number", "Правила сандхи", filtered);
    }

    private String resolveTitle(String topicCode) {
        return switch (topicCode) {
            case "sandhi-vowels-external" -> "Внешние сандхи: гласные";
            case "sandhi-consonants" -> "Внешние сандхи: согласные";
            case "sandhi-visarga" -> "Внешние сандхи: висарга";
            case "sandhi-vowels-internal" -> "Внутренние сандхи: гласные";
            case "sandhi-consonants-internal" -> "Внутренние сандхи: согласные";
            default -> topicCode;
        };
    }
}