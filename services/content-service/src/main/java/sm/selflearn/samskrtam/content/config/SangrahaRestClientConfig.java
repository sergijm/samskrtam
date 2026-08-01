package sm.selflearn.samskrtam.content.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Конфигурация RestClient для service-to-service вызовов к sangraha-service.
 * Адрес sangraha-service передаётся через env SANGRAHA_SERVICE_URL.
 */
@Configuration
public class SangrahaRestClientConfig {

    @Bean
    public RestClient sangrahaRestClient(@Value("${app.sangraha-service.url}") String sangrahaServiceUrl) {
        return RestClient.builder()
                .baseUrl(sangrahaServiceUrl)
                .build();
    }
}