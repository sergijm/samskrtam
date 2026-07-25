package sm.selflearn.samskrtam.content.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.content.dto.SangrahaVocabularyResponse;
import sm.selflearn.samskrtam.content.service.VocabularySyncService;
import sm.selflearn.samskrtam.sangraha.event.SangrahaVocabularyEvent;

/**
 * Внутренний контроллер для приёма словаря из sangraha-service.
 * Заменяет бывший @KafkaListener на топике sangraha-vocabulary-events.
 *
 * Не публичный и не ADMIN-эндпоинт — service-to-service вызов.
 * Вызывается sangraha-service напрямую (минуя Gateway) по env CONTENT_SERVICE_URL.
 */
@Slf4j
@RestController
@RequestMapping("/content/internal/sangraha")
@Tag(name = "Sangraha Internal", description = "Internal API for sangraha-service vocabulary sync")
@RequiredArgsConstructor
public class SangrahaVocabularyController {

    private final VocabularySyncService vocabularySyncService;

    @PostMapping("/vocabulary")
    @Operation(summary = "Synchronize vocabulary words from verse analysis",
               description = "Called by sangraha-service after LLM analysis of a verse. "
                           + "Idempotent: dedup by (wordIast, stem). Returns vocabularyWordId for each word.")
    @ApiResponse(responseCode = "200", description = "Words synced successfully")
    @ApiResponse(responseCode = "400", description = "Invalid payload")
    @ApiResponse(responseCode = "500", description = "DB error — transaction rolled back, nothing created")
    public SangrahaVocabularyResponse syncVocabulary(@RequestBody SangrahaVocabularyEvent event) {
        log.info("Received sangraha vocabulary REST request: verseId={}, workSlug={}, chapterSlug={}, wordsCount={}",
                event.getVerseId(), event.getWorkSlug(), event.getChapterSlug(),
                event.getWords() != null ? event.getWords().size() : 0);

        try {
            return vocabularySyncService.processEvent(event);
        } catch (Exception e) {
            log.error("Failed to process sangraha vocabulary request for verseId={}: {}",
                    event.getVerseId(), e.getMessage(), e);
            throw e;
        }
    }
}
