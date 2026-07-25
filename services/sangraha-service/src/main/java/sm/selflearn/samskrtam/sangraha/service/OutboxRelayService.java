package sm.selflearn.samskrtam.sangraha.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import sm.selflearn.samskrtam.content.dto.SangrahaVocabularyResponse;
import sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent;
import sm.selflearn.samskrtam.sangraha.model.OutboxEvent;
import sm.selflearn.samskrtam.sangraha.model.VerseWord;
import sm.selflearn.samskrtam.sangraha.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.sangraha.repository.VerseWordRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Outbox Relay — заменяет бывший Kafka-producer на синхронный REST-вызов content-service.
 *
 * Паттерн Transactional Outbox сохранён: OutboxEvent(VERSE_VOCABULARY_EXTRACTED)
 * по-прежнему пишется в той же транзакции, что и VerseWord[].
 * Изменился только транспорт: вместо Kafka — RestClient → POST CONTENT_SERVICE_URL.
 *
 * @Scheduled поллер опрашивает outbox_events (status=PENDING, aggregate_type=Verse,
 *           event_type=VERSE_VOCABULARY_EXTRACTED).
 */
@Slf4j
@Service
public class OutboxRelayService {

    private final OutboxEventRepository outboxEventRepository;
    private final VerseWordRepository verseWordRepository;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private static final int MAX_RETRIES = 5;

    public OutboxRelayService(
            OutboxEventRepository outboxEventRepository,
            VerseWordRepository verseWordRepository,
            ObjectMapper objectMapper,
            @Value("${app.content-service.url}") String contentServiceUrl) {
        this.outboxEventRepository = outboxEventRepository;
        this.verseWordRepository = verseWordRepository;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(contentServiceUrl)
                .build();
    }

    /**
     * Опрашивает outbox_events каждые N мс (настраивается через app.outbox.fixed-delay-ms).
     * Для каждой PENDING-записи типа VERSE_VOCABULARY_EXTRACTED:
     * 1. Десериализует payload
     * 2. POST /content/internal/sangraha/vocabulary
     * 3. При успехе (2xx) — проставляет VerseWord.vocabularyWordId по ответу, статус PUBLISHED
     * 4. При ошибке (4xx/5xx/timeout) — retry_count++, при исчерпании — FAILED
     */
    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:5000}")
    @Transactional
    public void relayPendingEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findAllPending();

        for (OutboxEvent event : pendingEvents) {
            if (!"VERSE_VOCABULARY_EXTRACTED".equals(event.getEventType())) {
                log.debug("Skipping outbox event with unknown type: {}", event.getEventType());
                continue;
            }

            try {
                SangrahaVocabularyEvent vocabEvent = objectMapper.readValue(
                        event.getPayload(), SangrahaVocabularyEvent.class);

                SangrahaVocabularyResponse response = restClient.post()
                        .uri("/content/internal/sangraha/vocabulary")
                        .body(vocabEvent)
                        .retrieve()
                        .body(SangrahaVocabularyResponse.class);

                if (response != null && response.getWords() != null) {
                    // Сопоставляем ответ с VerseWord этого стиха.
                    // Ответ идёт в том же порядке, что и words[] запроса.
                    List<VerseWord> verseWords = verseWordRepository
                            .findAllByVerseIdOrderByPositionAsc(vocabEvent.getVerseId());
                    Map<String, UUID> wordToVocabId = response.getWords().stream()
                            .collect(Collectors.toMap(
                                    e -> e.getWordIast() + "|" + e.getStem(),
                                    SangrahaVocabularyResponse.WordEntry::getVocabularyWordId,
                                    (a, b) -> a));

                    for (VerseWord vw : verseWords) {
                        String key = vw.getLemmaIast() + "|" + vw.getStem();
                        UUID vocabId = wordToVocabId.get(key);
                        if (vocabId != null) {
                            vw.setVocabularyWordId(vocabId);
                            vw.setVocabSyncStatus("SYNCED");
                            verseWordRepository.save(vw);
                        }
                    }
                }

                event.setStatus("PUBLISHED");
                event.setProcessedAt(Instant.now());
                outboxEventRepository.save(event);

                log.info("Relayed outbox event to REST: verseId={}, wordsSynced={}",
                        vocabEvent.getVerseId(),
                        response != null && response.getWords() != null ? response.getWords().size() : 0);

            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(truncate(e.getMessage(), 2000));
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus("FAILED");
                    log.error("Outbox event FAILED after {} retries: aggregateId={}, error={}",
                            MAX_RETRIES, event.getAggregateId(), e.getMessage(), e);
                } else {
                    log.warn("Outbox event retry {}/{}: aggregateId={}, error={}",
                            event.getRetryCount(), MAX_RETRIES, event.getAggregateId(), e.getMessage());
                }
                outboxEventRepository.save(event);
            }
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }
}
