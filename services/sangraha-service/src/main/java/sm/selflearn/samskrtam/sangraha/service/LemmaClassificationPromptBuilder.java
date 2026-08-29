package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.model.CurriculumSemanticClass;
import sm.selflearn.samskrtam.sangraha.model.Lemma;
import sm.selflearn.samskrtam.sangraha.repository.CurriculumSemanticClassRepository;

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
    private final CurriculumSemanticClassRepository topicRepository;

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
     * Вход батча: одна строка на лему — индекс, lemmaId, форма, POS/gender из
     * статистики (lemma, gender), до 2 примеров употребления из корпуса (§2.2).
     */
    public String buildUserPrompt(List<LemmaBatchItem> items) {
        StringBuilder sb = new StringBuilder("Classify the following Sanskrit lemmas.\n\n");
        for (int i = 0; i < items.size(); i++) {
            LemmaBatchItem item = items.get(i);
            sb.append("[").append(i).append("]\n");
            sb.append("lemmaId: ").append(item.lemma().getId()).append("\n");
            sb.append("lemmaIast: ").append(item.lemma().getLemmaIast()).append("\n");
            sb.append("lemmaDevanagari: ").append(item.lemma().getLemmaDevanagari()).append("\n");
            if (item.dominantPosCode() != null) {
                sb.append("dominantPosCode: ").append(item.dominantPosCode()).append("\n");
            }
            if (item.gender() != null) {
                sb.append("gender: ").append(item.gender()).append("\n");
            }
            appendExamples(sb, item.examples());
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

    private String renderCategories(List<CurriculumSemanticClass> topics) {
        Map<String, List<CurriculumSemanticClass>> byParent = topics.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getParent() == null ? "" : t.getParent().getCode()));
        StringBuilder sb = new StringBuilder();
        for (CurriculumSemanticClass root : byParent.getOrDefault("", List.of())) {
            sb.append("- ").append(root.getCode())
                    .append(" [").append(root.getLabelRu()).append(" / ").append(root.getLabelEn()).append("]")
                    .append(root.getDescription() == null ? "" : " — " + root.getDescription())
                    .append("\n");
            for (CurriculumSemanticClass leaf : byParent.getOrDefault(root.getCode(), List.of())) {
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

    /** Одна лема + данные её статистики (lemma, gender) + примеры для user-промпта. */
    public record LemmaBatchItem(Lemma lemma, String gender, String dominantPosCode, List<String> examples) {
    }
}