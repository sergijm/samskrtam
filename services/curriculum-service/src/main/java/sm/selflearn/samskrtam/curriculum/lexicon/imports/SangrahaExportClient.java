package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

/**
 * HTTP-клиент к sangraha-service: постраничный экспорт агрегированных лемм
 * (GET /sangraha/internal/lexicon/lemmas/export).
 * Если {@code sangraha-service.url} не задан — импорт отключается (пустые страницы).
 */
@Slf4j
@Component
public class SangrahaExportClient {

    private final RestClient restClient;
    private final boolean enabled;

    public SangrahaExportClient(RestClient.Builder builder,
                                @Value("${sangraha-service.url:}") String sangrahaUrl) {
        this.enabled = sangrahaUrl != null && !sangrahaUrl.isBlank();
        this.restClient = enabled ? builder.baseUrl(sangrahaUrl).build() : null;
        log.info("SangrahaExportClient enabled={}", enabled);
    }

    public LemmaExportPage fetchLemmaExport(UUID cursor, int limit) {
        if (!enabled) {
            return new LemmaExportPage(List.of(), null);
        }
        String uri = UriComponentsBuilder.fromPath("/sangraha/internal/lexicon/lemmas/export")
                .queryParam("cursor", cursor == null ? "" : cursor)
                .queryParam("limit", limit)
                .toUriString();
        try {
            return restClient.get().uri(uri).retrieve().body(LemmaExportPage.class);
        } catch (Exception e) {
            log.warn("Failed to fetch lemma export page (cursor={}): {}", cursor, e.getMessage());
            return new LemmaExportPage(List.of(), null);
        }
    }
}