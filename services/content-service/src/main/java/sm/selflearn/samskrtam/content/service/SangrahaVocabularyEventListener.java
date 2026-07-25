package sm.selflearn.samskrtam.content.service;

/**
 * УДАЛЁН (заменён на SangrahaVocabularyController — REST вместо Kafka).
 *
 * Раньше: @KafkaListener на топике sangraha-vocabulary-events.
 * Теперь: sangraha-service вызывает POST /content/internal/sangraha/vocabulary
 *         напрямую через RestClient (см. sangraha-service OutboxRelayService).
 *
 * Файл оставлен пустым классом как заглушка — удалить в следующем PR.
 */
@Deprecated(forRemoval = true)
public class SangrahaVocabularyEventListener {
}