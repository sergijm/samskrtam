package sm.selflearn.samskrtam.sangraha.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.content.event.VocabularyWordSyncedEvent;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;

/**
 * Принимает ack-события от content-service после синхронизации слов.
 * Идемпотентно обновляет verseWordId и vocabSyncStatus для каждого слова стиха.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VocabularySyncAckListener {

    private final VerseWordRepository verseWordRepository;

    @KafkaListener(
            topics = "vocabulary-sync-results",
            groupId = "sangraha-service",
            containerFactory = "sangrahaKafkaListenerContainerFactory"
    )
    @Transactional
    public void onAck(@Payload VocabularyWordSyncedEvent event, Acknowledgment ack) {
        try {
            log.info("Received vocabulary-sync ack: verseId={}, wordsCount={}",
                    event.getVerseId(), event.getWords() != null ? event.getWords().size() : 0);

            if (event.getWords() != null) {
                for (VocabularyWordSyncedEvent.WordSync w : event.getWords()) {
                    verseWordRepository.updateVocabularySync(
                            w.getVerseWordId(),
                            w.getVocabularyWordId(),
                            "SYNCED");
                }
            }

            ack.acknowledge();
            log.debug("Acknowledged vocabulary-sync ack for verseId={}", event.getVerseId());
        } catch (Exception e) {
            log.error("Failed to process vocabulary-sync ack for verseId={}: {}",
                    event.getVerseId(), e.getMessage(), e);
            // log-and-skip: коммитим, т.к. идемпотентно и можно догнать повторным анализом
            ack.acknowledge();
        }
    }
}
