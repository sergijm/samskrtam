package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent;

@Slf4j
@Component
@RequiredArgsConstructor
public class SangrahaVocabularyEventListener {

    private final VocabularySyncService vocabularySyncService;

    /**
     * Первый @KafkaListener в проекте (content-service — первый consumer).
     * Топик: sangraha-vocabulary-events
     * Группа: content-service
     *
     * Идемпотентность обеспечивается бизнес-логикой VocabularySyncService:
     * - VocabularyCategory: upsert по code (findByCodeIgnoreCase)
     * - Quiz: findBySlug — если есть, ничего не делаем
     * - VocabularyWord: dedup по (wordIast, stem)
     * - VocabularyWordCategory: existsById + только тогда insert
     *
     * Обработка ошибок: log-and-skip (DLQ — открытый вопрос, не блокирует M8).
     * Retry-механизм не реализован; при ошибке сообщение коммитится и теряется.
     */
    @KafkaListener(
            topics = "sangraha-vocabulary-events",
            groupId = "content-service",
            containerFactory = "kafkaListenerContainerFactory",
            properties = {
                    "max.poll.interval.ms:300000",
                    "max.poll.records:10"
            }
    )
    public void onVocabularyEvent(@Payload SangrahaVocabularyEvent event, Acknowledgment ack) {
        try {
            log.info("Received sangraha-vocabulary event: verseId={}, workSlug={}, chapterSlug={}, wordsCount={}",
                    event.getVerseId(), event.getWorkSlug(), event.getChapterSlug(),
                    event.getWords() != null ? event.getWords().size() : 0);

            vocabularySyncService.processEvent(event);

            ack.acknowledge();
            log.debug("Acknowledged sangraha-vocabulary event for verseId={}", event.getVerseId());
        } catch (Exception e) {
            log.error("Failed to process sangraha-vocabulary event for verseId={}: {}", event.getVerseId(), e.getMessage(), e);
            // log-and-skip: коммитим, чтобы не блокировать очередь
            // TODO: DLQ-топик sangraha-vocabulary-events-dlq (см. §11 content-service.md)
            ack.acknowledge();
        }
    }
}