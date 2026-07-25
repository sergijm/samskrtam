package sm.selflearn.samskrtam.content.config;

import lombok.extern.slf4j.Slf4j;

/**
 * УДАЛЁН (sangraha-vocabulary-events больше не потребляется через Kafka).
 *
 * Раньше: настраивал Kafka consumer для SangrahaVocabularyEventListener.
 * Теперь: sangraha-service вызывает REST напрямую — Kafka consumer
 *         content-service для sangraha-событий не нужен.
 *
 * Файл оставлен пустым классом как заглушка — удалить в следующем PR.
 */
@Slf4j
@Deprecated(forRemoval = true)
public class KafkaConsumerConfig {
}