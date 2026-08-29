package sm.selflearn.samskrtam.sangraha.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import sm.selflearn.samskrtam.content.dto.VerseLemmaBatch;

/**
 * Синхронный клиент curriculum-service для инкрементальных пачек лемм
 * (lexicon-content-pipeline.md §7): POST /api/v2/lexicon/import/verse-batch.
 * Если {@code app.curriculum-service.url} не задан — push отключается.
 * Сбой curriculum-service не должен валить анализ стиха — ошибки только логируются.
 */
@Slf4j
@Component
public class CurriculumLexiconClient {

    private final RestClient restClient;
    private final boolean enabled;

    public CurriculumLexiconClient(RestClient.Builder builder,
                                   @Value("${app.curriculum-service.url:}") String curriculumUrl) {
        this.enabled = curriculumUrl != null && !curriculumUrl.isBlank();
        this.restClient = enabled ? builder.baseUrl(curriculumUrl).build() : null;
        log.info("CurriculumLexiconClient enabled={}", enabled);
    }

    public void pushVerseBatch(VerseLemmaBatch batch) {
        if (!enabled) {
            log.warn("curriculum-service.url not configured, skipping verse batch push for verse {}",
                    batch.verseId());
            return;
        }
        try {
            restClient.post()
                    .uri("/api/v2/lexicon/import/verse-batch")
                    .body(batch)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Verse batch pushed to curriculum-service for verse {}", batch.verseId());
        } catch (Exception e) {
            log.warn("Failed to push verse batch for verse {}: {}", batch.verseId(), e.getMessage());
        }
    }
}