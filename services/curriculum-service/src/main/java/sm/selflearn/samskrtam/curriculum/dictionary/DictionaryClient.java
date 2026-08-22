package sm.selflearn.samskrtam.curriculum.dictionary;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import sm.selflearn.samskrtam.content.dto.frisch.FrischEntryDto;

import java.util.Arrays;
import java.util.List;

/**
 * HTTP-клиент к dictionary-service за данными словаря Фриша
 * (эндпоинт GET /api/v1/dictionary/frisch). Используется, чтобы подтянуть
 * переводы и грамматическую информацию (часть речи, род, парадигмы глагола)
 * для лемм, отображаемых на страницах парадигм склонения.
 *
 * <p>Если URL dictionary-service не задан ({@code dictionary-service.url} пуст),
 * вызовы возвращают пустой результат — страницы парадигм продолжают работать
 * на локальных данных лексикона.
 */
@Slf4j
@Component
public class DictionaryClient {

    private final RestClient restClient;
    private final boolean enabled;

    public DictionaryClient(RestClient.Builder builder,
                           @Value("${dictionary-service.url:}") String dictionaryUrl) {
        this.enabled = dictionaryUrl != null && !dictionaryUrl.isBlank();
        this.restClient = enabled ? builder.baseUrl(dictionaryUrl).build() : null;
        log.info("DictionaryClient enabled={}", enabled);
    }

    /**
     * @param lemma лемма в IAST (ударения опциональны)
     * @return список записей словаря Фриша (пустой, если сервис отключён или недоступен)
     */
    public List<FrischEntryDto> getFrischLemma(String lemma) {
        if (!enabled) {
            return List.of();
        }
        String uri = UriComponentsBuilder.fromPath("/api/v1/dictionary/frisch")
                .queryParam("lemma", lemma)
                .toUriString();
        try {
            FrischEntryDto[] entries = restClient.get().uri(uri).retrieve()
                    .body(FrischEntryDto[].class);
            return entries == null ? List.of() : Arrays.asList(entries);
        } catch (Exception e) {
            log.warn("Failed to fetch frisch lemma '{}' from dictionary-service: {}", lemma, e.getMessage());
            return List.of();
        }
    }
}
