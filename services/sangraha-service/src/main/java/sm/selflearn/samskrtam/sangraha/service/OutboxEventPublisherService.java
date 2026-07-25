package sm.selflearn.samskrtam.sangraha.service;

import lombok.extern.slf4j.Slf4j;

/**
 * УДАЛЁН (заменён на OutboxRelayService — REST вместо Kafka).
 *
 * Раньше: @Scheduled поллер, публиковавший outbox_events в Kafka
 *         (топик sangraha-vocabulary-events).
 * Теперь: OutboxRelayService делает синхронный REST-вызов
 *         POST {CONTENT_SERVICE_URL}/content/internal/sangraha/vocabulary
 *         и обрабатывает ответ (vocabularyWordId[]).
 *
 * Файл оставлен пустым классом как заглушка — удалить в следующем PR.
 */
@Slf4j
@Deprecated(forRemoval = true)
public class OutboxEventPublisherService {
}