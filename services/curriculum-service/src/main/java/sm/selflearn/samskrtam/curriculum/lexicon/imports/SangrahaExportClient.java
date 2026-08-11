package sm.selflearn.samskrtam.curriculum.lexicon.imports;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

/**
 * HTTP-клиент к sangraha-service за постраничным экспортом VerseWord[]
 * (internal-эндпоинт GET /sangraha/internal/content/verse-words/export),
 * см. lexicon-content-pipeline.md §2. Если {@code sangraha-service.url} не задан —
 * импорт безопасно отключается (пустые страницы).
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

    public VerseWordExportPage fetchPage(UUID cursor, int limit) {
        if (!enabled) {
            return new VerseWordExportPage(List.of(), null);
        }
        String uri = UriComponentsBuilder.fromPath("/sangraha/internal/content/verse-words/export")
                .queryParam("cursor", cursor == null ? "" : cursor)
                .queryParam("limit", limit)
                .toUriString();
        try {
            return restClient.get().uri(uri).retrieve().body(VerseWordExportPage.class);
        } catch (Exception e) {
            log.warn("Failed to fetch verse word export page (cursor={}): {}", cursor, e.getMessage());
            return new VerseWordExportPage(List.of(), null);
        }
    }
}