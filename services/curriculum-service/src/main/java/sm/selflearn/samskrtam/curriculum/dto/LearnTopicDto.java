package sm.selflearn.samskrtam.curriculum.dto;

import java.util.List;
import java.util.UUID;

/**
 * A topic card on the learning map page. `id`/`code` is the stable topic slug
 * (e.g. `a-stem-masc`), `prerequisites` reference other topics by code.
 */
public record LearnTopicDto(
        UUID id,
        String code,
        String titleRu,
        String titleEn,
        TopicTypeGroup typeGroup,
        String route,
        LearnTopicStatus status,
        Integer progressPercent,
        List<String> prerequisites
) {
}