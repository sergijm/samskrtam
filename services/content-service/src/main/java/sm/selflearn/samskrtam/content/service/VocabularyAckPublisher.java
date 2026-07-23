package sm.selflearn.samskrtam.content.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.content.event.VocabularyWordSyncedEvent;

import java.util.List;
import java.util.UUID;

/**
 * Публикует ack-событие VocabularyWordSyncedEvent в топик vocabulary-sync-results
 * после успешного сохранения слов в content-service.
 *
 * Публикация без Outbox (at-least-once) — допустимо, т.к. sangraha-service
 * обрабатывает событие идемпотентно (upsert по verseWordId).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VocabularyAckPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(UUID verseId, List<VocabularyWordSyncedEvent.WordSync> words) {
        var event = VocabularyWordSyncedEvent.builder()
                .verseId(verseId)
                .words(words)
                .build();
        kafkaTemplate.send("vocabulary-sync-results", verseId.toString(), event);
        log.info("Published vocabulary-sync ack: verseId={}, wordsCount={}", verseId, words.size());
    }
}
