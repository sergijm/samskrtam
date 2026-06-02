package sm.selflearn.samskrtam.gateway.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient бины для вызовов к Keycloak token endpoint и user-service.
 *
 * <p>Отдельные бины с разными base URL позволяют Spring Boot
 * автоматически добавлять traceparent заголовок (W3C propagation)
 * при наличии micrometer-tracing-bridge-otel в classpath.
 */
@Configuration
public class WebClientConfig {

    // Fallback/stub ObservationRegistry in case auto-configuration doesn't provide one
    @Bean
    public ObservationRegistry observationRegistry() {
        return ObservationRegistry.NOOP;
    }

    @Bean("keycloakWebClient")
    public WebClient keycloakWebClient(
            OAuth2Properties props,
            ObservationRegistry observationRegistry) {
        return WebClient.builder()
                .baseUrl(props.getInternalBaseUrl())
                .observationRegistry(observationRegistry)
                .build();
    }

    @Bean("userServiceWebClient")
    public WebClient userServiceWebClient(
            @Value("${services.user-service-url}") String userServiceUrl,
            ObservationRegistry observationRegistry) {
        return WebClient.builder()
                .baseUrl(userServiceUrl)
                .observationRegistry(observationRegistry)
                .build();
    }
}
