package sm.selflearn.samskrtam.sangraha.service;

import lombok.extern.slf4j.Slf4j;

/**
 * УДАЛЁН (заменён на прямую обработку HTTP-ответа в OutboxRelayService).
 *
 * Раньше: @KafkaListener на топике vocabulary-sync-results,
 *         принимал VocabularyWordSyncedEvent от content-service,
 *         обновлял VerseWord.vocabularyWordId.
 * Теперь: vocabularyWordId возвращаются синхронно в теле HTTP-ответа
 *         POST /content/internal/sangraha/vocabulary —
 *         OutboxRelayService сам проставляет VerseWord.vocabularyWordId.
 *
 * Файл оставлен пустым классом как заглушка — удалить в следующем PR.
 */
@Slf4j
@Deprecated(forRemoval = true)
public class VocabularySyncAckListener {
}
