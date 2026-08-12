package sm.selflearn.samskrtam.curriculum.questgen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questitem.repository.QuestItemRepository;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Batch regeneration orchestration for materialized quest items. Holds the
 * registry {@code Map<topic slug, QuizItemGenerator>} (built from all
 * {@link QuizItemGenerator} beans at startup) and, on {@code regenerate()},
 * clears the whole {@code quest_item} table and re-generates items topic by
 * topic. Returns per-topic statistics: for every topic with a registered
 * generator, the number of items added and the number of distinct
 * {@code progress_tag}s.
 */
@Service
@Slf4j
public class QuizItemGenerationService {

    private final TopicRepository topicRepository;
    private final QuestItemRepository questItemRepository;
    private final Map<String, QuizItemGenerator> generators;

    public QuizItemGenerationService(List<QuizItemGenerator> generatorBeans,
                                     TopicRepository topicRepository,
                                     QuestItemRepository questItemRepository) {
        this.topicRepository = topicRepository;
        this.questItemRepository = questItemRepository;
        this.generators = generatorBeans.stream()
                .flatMap(g -> g.supportedTopicSlugs().stream().map(slug -> Map.entry(slug, g)))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
    }

    /**
     * Clears the whole {@code quest_item} table and regenerates items for every
     * topic that has a registered generator (topics without one are skipped).
     *
     * @return topic slug → {@code {"generated": items added, "uniqueProgressTags": distinct progress tags}}
     */
    @Transactional
    public Map<String, Map<String, Integer>> regenerate() {
        int deleted = questItemRepository.deleteAllQuestItems();
        log.info("Cleared {} quest items, regenerating by topic", deleted);

        Map<String, Map<String, Integer>> stats = new LinkedHashMap<>();
        for (Topic topic : topicRepository.findAll()) {
            QuizItemGenerator generator = generators.get(topic.getCode());
            if (generator == null) {
                continue;
            }
            int count = generator.generate(topic);
            int uniqueProgressTags = (int) questItemRepository.countDistinctProgressTagByTopicId(topic.getId());
            stats.put(topic.getCode(), Map.of("generated", count, "uniqueProgressTags", uniqueProgressTags));
            log.info("Regenerated {} quest items for topic {} ({} distinct progress tags)",
                    count, topic.getCode(), uniqueProgressTags);
        }
        return stats;
    }
}