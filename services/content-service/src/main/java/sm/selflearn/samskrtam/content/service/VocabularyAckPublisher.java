package sm.selflearn.samskrtam.content.service;

import lombok.extern.slf4j.Slf4j;

/**
 * УДАЛЁН (заменён на прямой REST-ответ SangrahaVocabularyResponse).
 *
 * Раньше: публиковал VocabularyWordSyncedEvent в топик vocabulary-sync-results
 *         для обратного подтверждения sangraha-service.
 * Теперь: vocabularyWordId возвращаются синхронно в теле HTTP-ответа
 *         POST /content/internal/sangraha/vocabulary — ack-топик не нужен.
 *
 * Файл оставлен пустым классом как заглушка — удалить в следующем PR.
 */
@Slf4j
@Deprecated(forRemoval = true)
public class VocabularyAckPublisher {
}
