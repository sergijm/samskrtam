package sm.selflearn.samskrtam.sangraha.config;

import lombok.extern.slf4j.Slf4j;

/**
 * УДАЛЁН (sangraha-service больше не producer и не consumer Kafka).
 *
 * Раньше: настраивал топик sangraha-vocabulary-events и
 *         sangrahaKafkaListenerContainerFactory для VocabularySyncAckListener.
 * Теперь: весь обмен с content-service — через RestClient
 *         (см. OutboxRelayService).
 *
 * Файл оставлен пустым классом как заглушка — удалить в следующем PR.
 */
@Slf4j
@Deprecated(forRemoval = true)
public class KafkaConfig {
}