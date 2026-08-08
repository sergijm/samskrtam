package sm.selflearn.samskrtam.curriculum.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP-клиент к sangraha-service за кандидатами существительных для бутстрапа
 * склонений (internal-эндпоинт GET /sangraha/internal/content/nominal-lemmas).
 * Если URL sangraha не задан ({@code sangraha-service.url} пуст), вызовы
 * возвращают пустой результат — бутстрап безопасно отключается.
 */
@Slf4j
@Component
public class SangrahaClient {

    private final RestClient restClient;
    private final boolean enabled;

    public SangrahaClient(RestClient.Builder builder,
                          @Value("${sangraha-service.url:}") String sangrahaUrl) {
        this.enabled = sangrahaUrl != null && !sangrahaUrl.isBlank();
        this.restClient = enabled ? builder.baseUrl(sangrahaUrl).build() : null;
        log.info("SangrahaClient enabled={}", enabled);
    }

    /**
     * @param stemClass ограничение по классу основы (null — все классы)
     * @param limit     максимальное число кандидатов
     * @return список кандидатов (пустой, если sangraha отключён)
     */
    public NominalLemmaCandidatesResponse fetchCandidates(String stemClass, int limit) {
        if (!enabled) {
            return new NominalLemmaCandidatesResponse(java.util.List.of());
        }
        String uri = UriComponentsBuilder.fromPath("/sangraha/internal/content/nominal-lemmas")
                .queryParam("stemClass", stemClass == null ? "" : stemClass)
                .queryParam("limit", limit)
                .toUriString();
        try {
            return restClient.get().uri(uri).retrieve().body(NominalLemmaCandidatesResponse.class);
        } catch (Exception e) {
            log.warn("Failed to fetch nominal lemma candidates from sangraha (stemClass={}): {}",
                    stemClass, e.getMessage());
            return new NominalLemmaCandidatesResponse(java.util.List.of());
        }
    }
}