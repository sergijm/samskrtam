package sm.selflearn.samskrtam.content.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Событие, подтверждающее синхронизацию стиховых слов (verseWordId)
 * с лексическими словами (vocabularyWordId) после обработки SangrahaVocabularyEvent.
 *
 * Producer  — content-service (после сохранения VocabularyWord).
 * Consumer  — sangraha-service (для сохранения обратной ссылки).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyWordSyncedEvent {

    private UUID verseId;
    private List<WordSync> words;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordSync {
        private UUID verseWordId;
        private UUID vocabularyWordId;
    }
}
