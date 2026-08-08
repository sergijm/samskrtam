package sm.selflearn.samskrtam.curriculum.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.curriculum.model.Topic;
import sm.selflearn.samskrtam.curriculum.questgen.DeclensionQuestItemBatchGenerator;
import sm.selflearn.samskrtam.curriculum.repository.TopicRepository;

import java.util.List;

/**
 * При старте curriculum-service наполняет данные склонений, если их ещё нет:
 * импортирует существительные из sangraha для тем с {@code targetItemCount > 0}
 * и запускает генератор quest-элементов (квизы) для этих тем. Идемпотентно:
 * импорт пропускает морфо-классы, в которых уже есть лексемы, а генератор
 * не дублирует элементы (см. quest_item_generation_key).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeclensionBootstrapper implements ApplicationRunner {

    private final TopicRepository topicRepository;
    private final DeclensionDataImporter importer;
    private final DeclensionQuestItemBatchGenerator questItemGenerator;

    @Override
    public void run(ApplicationArguments args) {
        try {
            int imported = importer.importForPendingTopics();
            int generated = generateForTargetTopics();
            log.info("Declension bootstrap: imported {} lexemes, generated quest items for {} topics",
                    imported, generated);
        } catch (Exception e) {
            log.error("Declension bootstrap failed; service continues with empty declension data", e);
        }
    }

    @Transactional
    int generateForTargetTopics() {
        List<Topic> targets = topicRepository.findByTargetItemCountGreaterThan(0);
        int generated = 0;
        for (Topic topic : targets) {
            int created = questItemGenerator.generateForTopic(topic.getId());
            generated += created;
        }
        return targets.size();
    }
}