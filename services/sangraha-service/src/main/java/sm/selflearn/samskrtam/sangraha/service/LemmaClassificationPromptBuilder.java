package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.model.CurriculumSemanticTopic;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.repository.CurriculumSemanticTopicRepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Собирает system/user промпты для классификации лемм (lemma-classification.md
 * §2.1). Системный промпт — шаблон из {@code prompts/lemma-classification.md}
 * с подстановкой закрытого списка 42 категорий (§1.5), не статичный текст.
 * User-промпт — список лемм батча (индекс + lemmaId + описание по §2.2).
 */
@Component
@RequiredArgsConstructor
public class LemmaClassificationPromptBuilder {

    private final PromptLoader promptLoader;
    private final CurriculumSemanticTopicRepository topicRepository;

    /**
     * Секция ## system из шаблона + полный список категорий CURRICULUM.
     */
    public String buildSystemPrompt() {
        String template = promptLoader.getLemmaClassificationPrompt();
        String system = extractSystemSection(template);
        String categories = renderCategories(topicRepository.findAll());
        return system + "\n\n=== Категории CURRICULUM (закрытый список) ===\n" + categories;
    }

    /**
     * Вход батча: одна строка на лему — индекс, lemmaId, форма, POS/gender,
     * до 2 примеров употребления из корпуса (§2.2).
     */
    public String buildUserPrompt(List<LemmaBatchItem> items) {
        StringBuilder sb = new StringBuilder("Classify the following Sanskrit lemmas.\n\n");
        for (int i = 0; i < items.size(); i++) {
            LemmaBatchItem item = items.get(i);
            sb.append("[").append(i).append("]\n");
            sb.append("lemmaId: ").append(item.lemma.getId()).append("\n");
            sb.append("lemmaIast: ").append(item.lemma.getLemmaIast()).append("\n");
            sb.append("lemmaDevanagari: ").append(item.lemma.getLemmaDevanagari()).append("\n");
            if (item.lemma.getDominantPosCode() != null) {
                sb.append("dominantPosCode: ").append(item.lemma.getDominantPosCode()).append("\n");
            }
            if (item.lemma.getGender() != null) {
                sb.append("gender: ").append(item.lemma.getGender()).append("\n");
            }
            appendExamples(sb, item.examples);
            sb.append("\n");
        }
        return sb.toString();
    }

    private void appendExamples(StringBuilder sb, List<String> examples) {
        if (examples == null || examples.isEmpty()) {
            return;
        }
        sb.append("examples:\n");
        for (String example : examples) {
            sb.append("  - ").append(example).append("\n");
        }
    }

    private String renderCategories(List<CurriculumSemanticTopic> topics) {
        Map<String, List<CurriculumSemanticTopic>> byParent = topics.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getParent() == null ? "" : t.getParent().getCode()));
        StringBuilder sb = new StringBuilder();
        for (CurriculumSemanticTopic root : byParent.getOrDefault("", List.of())) {
            sb.append("- ").append(root.getCode())
                    .append(" [").append(root.getLabelRu()).append(" / ").append(root.getLabelEn()).append("]")
                    .append(root.getDescription() == null ? "" : " — " + root.getDescription())
                    .append("\n");
            for (CurriculumSemanticTopic leaf : byParent.getOrDefault(root.getCode(), List.of())) {
                sb.append("    - ").append(leaf.getCode())
                        .append(" [").append(leaf.getLabelRu()).append(" / ").append(leaf.getLabelEn()).append("]")
                        .append(leaf.getDescription() == null ? "" : " — " + leaf.getDescription())
                        .append("\n");
            }
        }
        return sb.toString();
    }

    private String extractSystemSection(String template) {
        int systemStart = template.indexOf("## system\n");
        if (systemStart < 0) {
            return template;
        }
        int codeStart = template.indexOf("```\n", systemStart);
        if (codeStart < 0) {
            return template;
        }
        int codeEnd = template.indexOf("\n```", codeStart + 5);
        if (codeEnd < 0) {
            return template;
        }
        return template.substring(codeStart + 5, codeEnd).trim();
    }

    /** Одна лема + её примеры для user-промпта. */
    public record LemmaBatchItem(Lemma lemma, List<String> examples) {
    }
}